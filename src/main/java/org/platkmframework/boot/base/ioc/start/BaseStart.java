/**
 * ****************************************************************************
 *  Copyright(c) 2025 the original author Eduardo Iglesias Taylor.
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
package org.platkmframework.boot.base.ioc.start;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.platkmframework.annotation.HttpRest;
import org.platkmframework.boot.base.init.BootInitializer;
import org.platkmframework.boot.base.ioc.BootInversionOfControl;
import org.platkmframework.context.ObjectContainer; 
import org.platkmframework.context.project.ContentPropertiesConstant;
import org.platkmframework.context.project.EnvironmentType;
import org.platkmframework.context.project.ProjectContent;
import org.platkmframework.core.rmi.RMIException;
import org.platkmframework.core.rmi.RMIServerManager;
import org.platkmframework.core.scheduler.SchedulerManager;
import org.platkmframework.doi.data.ObjectReferece;
import org.platkmframework.doi.exception.IoDCException;
import org.platkmframework.httpclient.proxy.HttpRestProxyProcessor;
import org.platkmframework.proxy.ProxyProcessorFactory;
import org.platkmframework.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *   Author:
 *     Eduardo Iglesias
 *   Contributors:
 *   	Eduardo Iglesias - initial API and implementation
 */
public class BaseStart {
	
	private static Logger logger = LoggerFactory.getLogger(BaseStart.class);
	
    /**
     * start 
     * @param appClass appClass
     * @param args args
     * @throws Exception Exception
     */
    public void start(Class appClass, String[] args) throws Exception {
        
    	if(appClass == null) {
        	logger.error("Application class should not be null");
        	System.exit(-1);
        }
    	
    	Properties properties = readDefaultPropertyFile(appClass, args);
    	initJson();
    	applyIoD(new BootInversionOfControl(), properties);
        initProxyProcessorFactory();
        initBootInitializer();
    }

    /**
     * readDefaultPropertyFile
     * @param appClass appClass
     * @param args args
     * @return Properties
     */
    protected Properties readDefaultPropertyFile(Class appClass, String[] args) {
		
    	String propertyFileName = "application.properties";
    	
    	Properties properties = new Properties();
    	if(args != null)
    		for (String elemento : args) {
                String[] partes = elemento.split("=", 2);
                if (partes.length == 2) {
                    String clave = partes[0].trim();
                    String valor = partes[1].trim();
                    properties.put(clave, valor);
                }
            }
    	if(properties.containsKey("platkmEnv")) {
    		EnvironmentType environmentType = EnvironmentType.valueOf(properties.getProperty("platkmEnv",""));
    		if(environmentType != null) {
    			propertyFileName = "application-" + environmentType.name() + ".properties";
    		}
    	}
    	loadApplicationProperties(propertyFileName, properties); 
    	
    	if(properties.containsKey(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_PACKAGE_PREFIX)) {
    		String ivdPackage = properties.getProperty(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_PACKAGE_PREFIX, "");
    		if(StringUtils.isBlank(ivdPackage)) {
    			properties.setProperty(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_PACKAGE_PREFIX, appClass.getPackageName());
    		}
    	}else
    		properties.setProperty(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_PACKAGE_PREFIX, appClass.getPackageName());
    	
    	ProjectContent.instance().putProperties(properties);
		return properties;
	}
    
    /**
     * load Application Properties
     * @param propertyFileName property File Name
     * @param properties properties
     */
    private void loadApplicationProperties(String propertyFileName, Properties properties) {
             
	    InputStream inputStream = this.getClass().getResourceAsStream("/"+propertyFileName.trim());
        if (inputStream == null) {
        	logger.error("properties file not found {}", propertyFileName);
        	System.exit(-1);
        }
        
        try {
			properties.load(inputStream);
		} catch (IOException e) {
			logger.error("error loading properties file {}, error details {}", propertyFileName, e.getMessage());
        	System.exit(-1);
		} 
           
    }

	/**
	 * apply IoD
	 * @param bootInversionOfControl boot Inversion Of Control
	 * @param prop prop
	 * @throws IoDCException IoDCException
	 */
    protected void applyIoD(BootInversionOfControl bootInversionOfControl, Properties prop) throws IoDCException {
        String javaClassPath = System.getProperty("java.class.path");
        String packagesPrefix = ProjectContent.instance().getProperty(ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_CONFIGURATION_PACKAGE_PREFIX);
        packagesPrefix += ",org.platkmframework";
        ObjectReferece objectReferece = new ObjectReferece();
        objectReferece.setProp(prop);
        bootInversionOfControl.process(javaClassPath, packagesPrefix.split(","), objectReferece);
        ObjectContainer.instance().setReference(objectReferece);
    }

    /**
     * initJson
     */
    protected void initJson() {
        JsonUtil.init(
        		StringUtils.isNotBlank(ProjectContent.instance().getDateFormat())?
        				ProjectContent.instance().getDateFormat():ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_JDBC_FORMAT_DATE_DEFAULT,
        		
        		StringUtils.isNotBlank(ProjectContent.instance().getDateTimeFormat())?
                		ProjectContent.instance().getDateTimeFormat(): ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_JDBC_FORMAT_DATE_TIME_DEFAULT,
                		
        		StringUtils.isNotBlank(ProjectContent.instance().getTimeFormat())?
                		ProjectContent.instance().getTimeFormat(): ContentPropertiesConstant.ORG_PLATKMFRAMEWORK_JDBC_FORMAT_TIME_DEFAULT);
    }

    /**
     * init Proxy Processor Factory
     * @throws RMIException RMIException
     */
    protected void initProxyProcessorFactory() throws RMIException {
        ProxyProcessorFactory.instance().register(HttpRest.class.getName(), new HttpRestProxyProcessor());
        RMIServerManager.instance().runAllOnStart();
        SchedulerManager.instance().runAllOnStart();
    }

    /**
     * initBootInitializer
     */
    protected void initBootInitializer() {
    	List<Object> list = ObjectContainer.instance().getListObjectByInstance(BootInitializer.class);
        for (Object initializer : list) {
            ((BootInitializer)initializer).process();
        }
    }

    /**
     * BaseStart
     */
    public BaseStart() {
        super();
    }
}
