/**
 * ****************************************************************************
 *  Copyright(c) 2023 the original author Eduardo Iglesias Taylor.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	 https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *  	Eduardo Iglesias Taylor - initial API and implementation
 * *****************************************************************************
 */
package org.platkmframework.boot.base.server.runner;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.platkmframework.boot.base.ioc.BootInversionOfControl;
import org.platkmframework.boot.base.server.filter.CORSFilter;
import org.platkmframework.boot.base.server.filter.servlet.IndexContentServlet;
import org.platkmframework.context.ObjectContainer;
import org.platkmframework.context.project.ContentPropertiesConstant;
import org.platkmframework.context.project.CorePropertyConstant;
import org.platkmframework.context.project.ProjectContent;
import org.platkmframework.security.content.filter.SecurityApiFilter;
import org.platkmframework.util.DataTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 *   Author:
 *     Eduardo Iglesias
 *   Contributors:
 *   	Eduardo Iglesias - initial API and implementation
 */
public class StartServerProcessor {

    /**
     * Atributo logger
     */
    private static Logger logger = LoggerFactory.getLogger(StartServerProcessor.class);

    /**
     * StartServerProcessor
     */
    private static final String C_DOS_WHITELIST = "DOS_whitelist";

    /**
     * start
     * @return Server
     * @throws Exception Exception
     */
    public Server start() throws Exception {
     
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_NAME), DataTypeUtil.getIntegerValue(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_PORT), 0));
        Server server = new Server(inetSocketAddress);
        server.setStopAtShutdown(true);
        if (Boolean.TRUE.equals(DataTypeUtil.getBooleanValue(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_ACTIVE_QUEUE_POOL), false))) {
            QueuedThreadPool threadPool = new QueuedThreadPool(DataTypeUtil.getIntegerValue(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_MAXTHREADS), 0), DataTypeUtil.getIntegerValue(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_MINTHREADS), 0), DataTypeUtil.getIntegerValue(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_IDLETIMEOUT), 0));
            server.addBean(threadPool);
        }
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_CONTENT_PATH, "/"));
        webapp.setResourceBase("");
        //webapp.setInitParameter(C_APPLICATION_ENVIRONMENT, StartConfig.getEnvironment());
        webapp.setDisplayName(org.platkmframework.core.request.servlet.RequestManagerServlet.class.getName());
        List<String> servletPatterns = addContentServlet(webapp);
        String[] patternsMainServlet = (ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVLET_PLATH, "") + "/*").split(",");
        servletPatterns.addAll(Arrays.asList(patternsMainServlet));
        jakarta.servlet.ServletRegistration.Dynamic dynamic;
        String strRequestManagerServlet = ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVLET_REQUEST_MANAGER, "");
        if (StringUtils.isNotBlank(strRequestManagerServlet)) {
            Class requestManagerServlet = Class.forName(strRequestManagerServlet);
            dynamic = webapp.getServletContext().addServlet(requestManagerServlet.getName(), (Servlet) requestManagerServlet.getDeclaredConstructors()[0].newInstance());
        } else {
            dynamic = webapp.getServletContext().addServlet(org.platkmframework.core.request.servlet.RequestManagerServlet.class.getName(), new org.platkmframework.core.request.servlet.RequestManagerServlet());
        }
        dynamic.addMapping(patternsMainServlet);
        dynamic.setLoadOnStartup(1);
        dynamic.setAsyncSupported(true);
        dynamic.setMultipartConfig(getMultipartConfig());
        //String indexPate = ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_INDEX_PAGE);
        //if(StringUtils.isNotBlank(indexPate))
        String[] patterns = new String[servletPatterns.size()];
        patterns = servletPatterns.toArray(patterns);
        //filter
        Filter filter = new org.eclipse.jetty.servlets.DoSFilter();
        updateDosFilterConfig((org.eclipse.jetty.servlets.DoSFilter) filter);
        webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, patterns));
        filter = new org.eclipse.jetty.servlets.QoSFilter();
        webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, patterns));
        String strCustomFilterCors = ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_CUSTOM_FILTER_CORS, "");
        if (StringUtils.isNotBlank(strCustomFilterCors)) {
            Class customFilterCorsClass = Class.forName(strCustomFilterCors);
            filter = (Filter) customFilterCorsClass.getDeclaredConstructors()[0].newInstance();
        } else {
            filter = new CORSFilter();
        }
        webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, patterns));
        filter = new org.platkmframework.core.request.filter.ExceptionFilter();
        webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, patterns));
        addSystemCustomFilter(webapp, patterns);
        filter = new SecurityApiFilter();
        webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, patterns));
        //List<Object> custonFilters = ObjectContainer.instance().getListObjectByAnnontation(CustomFilter.class);
        WebFilter webFilter;
        List<Object> objects = ObjectContainer.instance().getListObjectByAnnontation(WebFilter.class);
        if(objects != null)
	        for (Object object : objects){
	            filter = (Filter) object;
	            webFilter = filter.getClass().getAnnotation(WebFilter.class);
	            webapp.getServletHandler().addFilter(newFilterHolder(filter, true), newFilterMapping(filter, webFilter.urlPatterns()));
	        }
        webapp.setErrorHandler(new CustomErrorHandler());
        //webapp.setInitParameter(ServerUtil.C_PARAM_SECRET_KEY, secretKey);
        //(POST)/shutdown?token=
        List<Handler> handlers = new ArrayList<>();
        handlers.add(webapp);
        if (StringUtils.isNotBlank(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_STOPKEY))) {
            handlers.add(new ShutdownHandler(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_STOPKEY)));
        }
        Object obj = ObjectContainer.instance().getCustomInfoByKey(BootInversionOfControl.C_WEBSCOKET_ENDPOINT_CLASS);
        if (obj != null) {
            List<String> webSocketEndPointClasses = (List<String>) obj;
            if (!webSocketEndPointClasses.isEmpty()) {
                String appServerPort = ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_PORT);
                String appServerWebSocketPort = ProjectContent.instance().getProperty(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_WEBSOKET_SERVER_PORT);
                if (StringUtils.isBlank(appServerWebSocketPort))
                    appServerWebSocketPort = "8081";
                if (appServerWebSocketPort.trim().equals(appServerPort.trim())) {
                    logger.error("WebSocket configurarion error -> Los puertos de la aplicación principal y el websocket deben ser diferentes: " + appServerPort);
                    System.exit(-1);
                }
                InetSocketAddress inetSocketAddress1 = new InetSocketAddress(ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_NAME), DataTypeUtil.getIntegerValue(appServerWebSocketPort, 0));
                Server serverWebSocket = new Server(inetSocketAddress1);
                serverWebSocket.setStopAtShutdown(true);
                ServletContextHandler handler = new ServletContextHandler(serverWebSocket, "/ctx");
                JakartaWebSocketServletContainerInitializer.configure(handler, (servletContext, container) -> {
                    // Configure the ServerContainer.
                    container.setDefaultMaxTextMessageBufferSize(128 * 1024);
                    Class class1;
                    ServerEndpoint serverEndpoint;
                    for (String className : webSocketEndPointClasses) {
                        // Simple registration of your WebSocket endpoints.
                        try {
                            class1 = Class.forName(className);
                            //container.addEndpoint(Class.forName(className));
                            serverEndpoint = ((ServerEndpoint) class1.getAnnotation(ServerEndpoint.class));
                            // Advanced registration of your WebSocket endpoints.
                            container.addEndpoint(ServerEndpointConfig.Builder.create(class1, serverEndpoint.value()).subprotocols(List.of("my-ws-protocol")).build());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                serverWebSocket.setHandler(handler);
                serverWebSocket.start();
            }
        }
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(handlers.toArray(new Handler[handlers.size()]));
        server.setHandler(handlerCollection);
        String value = ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_MAX_FORM_CONTENT_SIZE);
        if (StringUtils.isNotBlank(value)) {
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", Double.valueOf(value));
        }
        addIndexServlet(webapp);
        try {
            server.start();
            //server.join();
            logger.info("Process started...: " + ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_NAME) + ":" + ProjectContent.instance().getProperty(CorePropertyConstant.ORG_PLATKMFRAMEWORK_SERVER_PORT, ""));
        } catch (Exception e) {
            server = null;
            e.printStackTrace();
            logger.error("Process error -> " + e.getMessage());
            System.exit(-1);
        }
        return server;
    }

    /**
     * updateDosFilterConfig
     * @param properties properties
     * @param filter filter
     */
    private void updateDosFilterConfig(org.eclipse.jetty.servlets.DoSFilter filter) {
        //
        String DOS_whitelist = ProjectContent.instance().getProperty(C_DOS_WHITELIST,"")!= "" ? ProjectContent.instance().getProperty(C_DOS_WHITELIST) : (System.getenv(C_DOS_WHITELIST) != null ? System.getenv(C_DOS_WHITELIST) : "");
        if (StringUtils.isNotBlank(DOS_whitelist)) {
            filter.setWhitelist(DOS_whitelist);
        }
    }

    /**
     * addSystemCustomFilter
     * @param webapp webapp
     * @param patterns patterns
     */
    public void addSystemCustomFilter(WebAppContext webapp, String[] patterns) {
    }

    /**
     * addContentServlet
     * @param webapp webapp
     * @return List
     */
    private List<String> addContentServlet(WebAppContext webapp) {
        List<String> patterns = new ArrayList<>();
        WebServlet webServletAnnotation;
        HttpServlet httpServlet;
        List<Object> objects = ObjectContainer.instance().getListObjectByAnnontation(WebServlet.class);
        if(objects != null)
	        for (Object object : objects) {
	            httpServlet = (HttpServlet) object;
	            webServletAnnotation = httpServlet.getClass().getAnnotation(WebServlet.class);
	            jakarta.servlet.ServletRegistration.Dynamic dynamic = webapp.getServletContext().addServlet(httpServlet.getClass().getName(), (HttpServlet) object);
	            dynamic.addMapping(webServletAnnotation.urlPatterns());
	            dynamic.setLoadOnStartup(1);
	            dynamic.setAsyncSupported(true);
	            patterns.addAll(Arrays.asList(webServletAnnotation.urlPatterns()));
	        }
        return patterns;
    }

    /**
     * addIndexServlet
     * @param webapp webapp
     */
    private void addIndexServlet(WebAppContext webapp) {
        //		jakarta.servlet.ServletRegistration.Dynamic dynamic = webapp.getServletContext().addServlet(EmptyRequestManagerServlet.class.getName(), new EmptyRequestManagerServlet());
        jakarta.servlet.ServletRegistration.Dynamic dynamic = webapp.getServletContext().addServlet(IndexContentServlet.class.getName(), new IndexContentServlet());
        dynamic.addMapping("/");
        dynamic.setLoadOnStartup(1);
        dynamic.setAsyncSupported(true);
    }

    /**
     * getMultipartConfig
     * @param properties properties
     * @return MultipartConfigElement
     */
    private MultipartConfigElement getMultipartConfig() {
        return new MultipartConfigElement("/tmp", 5242880, 20971520, 0);
    }

    /**
     * newFilterHolder
     * @param filter filter
     * @param asynSupport asynSupport
     * @return FilterHolder
     */
    protected FilterHolder newFilterHolder(Filter filter, boolean asynSupport) {
        FilterHolder filterHolder = new FilterHolder(filter);
        filterHolder.setName(filter.getClass().getName());
        filterHolder.setAsyncSupported(asynSupport);
        return filterHolder;
    }

    /**
     * newFilterMapping
     * @param filter filter
     * @param patterns patterns
     * @return FilterMapping
     */
    protected FilterMapping newFilterMapping(Filter filter, String[] patterns) {
        FilterMapping filterMapping = new FilterMapping();
        filterMapping.setFilterName(filter.getClass().getName());
        filterMapping.setPathSpecs(patterns);
        //filterMapping.setPathSpec(String.join(",", patterns));
        return filterMapping;
    }

    /**
     * Constructor StartServerProcessor
     */
    public StartServerProcessor() {
        super();
    }
}
