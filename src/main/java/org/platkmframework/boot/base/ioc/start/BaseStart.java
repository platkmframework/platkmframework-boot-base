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
package org.platkmframework.boot.base.ioc.start;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.platkmframework.annotation.HttpRest;
import org.platkmframework.boot.base.init.BootInitializer;
import org.platkmframework.boot.base.ioc.BootInversionOfControl;
import org.platkmframework.content.ObjectContainer;
import org.platkmframework.content.json.JsonUtil;
import org.platkmframework.content.project.ContentPropertiesConstant;
import org.platkmframework.content.project.ProjectContent;
import org.platkmframework.core.rmi.RMIException;
import org.platkmframework.core.rmi.RMIServerManager;
import org.platkmframework.core.scheduler.SchedulerManager;
import org.platkmframework.doi.data.ObjectReferece;
import org.platkmframework.doi.exception.IoDCException;
import org.platkmframework.httpclient.proxy.HttpRestProxyProcessor;
import org.platkmframework.proxy.ProxyProcessorFactory;
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
     * @throws Exception Exception
     */
    public void start(String[] args) throws Exception {
        
    	initJson();
    	Properties prop = readDefaultPropertyFile(args);
    	applyIoD(new BootInversionOfControl(), prop);
        initProxyProcessorFactory();
        initBootInitializer();
    }

    /**
     * readDefaultPropertyFile
     * @param args Properties
     */
    protected Properties readDefaultPropertyFile(String[] args) {
		
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
    		propertyFileName = "application-" + properties.containsKey("platkmEnv") + ".properties";
    	}
    	loadApplicationProperties(propertyFileName, properties);
    	ProjectContent.instance().putProperties(properties);
    	
		return properties;
	}
    
    /**
     * loadApplicationProperties
     * @param files files
     * @throws IOException IOException
     */
    private void loadApplicationProperties(String propertyFileName, Properties properties) {
             
	    InputStream inputStream = this.getClass().getResourceAsStream(propertyFileName.trim());
        if (inputStream == null) {
        	logger.error("properties file not found {}", propertyFileName);
        	System.exit(-1);
        }
        
        try {
			properties.load(inputStream);
		} catch (IOException e) {
			logger.error("properties file not found {}", propertyFileName);
        	System.exit(-1);
		} 
           
    }

	/**
     * applyIoD
     * @param bootInversionOfControl bootInversionOfControl
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
        JsonUtil.init();
    }

    /**
     * initProxyProcessorFactory
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
