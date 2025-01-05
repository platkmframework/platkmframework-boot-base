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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

/**
 *   Author:
 *     Eduardo Iglesias
 *   Contributors:
 *   	Eduardo Iglesias - initial API and implementation
 */
public class ServerUtil {

    /**
     * Atributo C_CONFIG_FILE
     */
    public static final String C_CONFIG_FILE = "config.file";

    /**
     * Atributo C_PARAM_HOST_NAME
     */
    public static final String C_PARAM_HOST_NAME = "hostname";

    /**
     * Atributo C_STOP_KEY
     */
    public static final String C_STOP_KEY = "stopKey";

    /**
     * Atributo C_PARAM_PORT
     */
    public static final String C_PARAM_PORT = "port";

    /**
     * Atributo C_DEFAULT_PORT
     */
    public static final String C_DEFAULT_PORT = "80";

    /**
     * ServerUtil
     */
    public static final String C_ENCODE = "UTF-8";

    /**
     * description: constructor
     */
    private ServerUtil() {
        throw new IllegalStateException("ServerUtil class");
    }

    /**
     * readConfig
     * @param currentFolder currentFolder
     * @return Properties
     */
    public static Properties readConfig(File currentFolder) {
        Properties prop = new Properties();
        //read  file from default path
        File configFile = new File(currentFolder.getAbsolutePath() + File.separator + C_CONFIG_FILE);
        if (configFile.exists() && configFile.isFile()) {
            try {
                List<?> list = FileUtils.readLines(configFile, "UTF-8");
                if (list != null)
                    for (int i = 0; i < list.size(); i++) {
                        String line = (String) list.get(i);
                        String[] keyValue = line.split("=");
                        prop.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
                    }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop;
    }
}
