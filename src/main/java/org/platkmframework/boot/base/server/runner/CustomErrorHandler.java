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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.platkmframework.content.json.JsonUtil;
import org.platkmframework.httpclient.error.ErrorInfo;
import org.platkmframework.util.JsonException;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.io.ByteBufferOutputStream;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

/**
 *   Author:
 *     Eduardo Iglesias
 *   Contributors:
 *   	Eduardo Iglesias - initial API and implementation
 */
public class CustomErrorHandler extends ErrorHandler {

    /**
     * errorPageForMethod
     * @param method method
     * @return boolean
     */
    @Override
    public boolean errorPageForMethod(String method) {
        switch(method) {
            case "GET":
            case "POST":
            case "HEAD":
            case "PUT":
            case "DELETE":
                return true;
            default:
                return false;
        }
    }

    /**
     * writeErrorPage
     * @param request request
     * @param writer writer
     * @param code code
     * @param message message
     * @param showStacks showStacks
     * @throws IOException IOException
     */
    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
        writer.write(message);
    }

    /**
     * handle
     * @param target target
     * @param baseRequest baseRequest
     * @param request request
     * @param response response
     * @throws IOException IOException
     * @throws ServletException ServletException
     */
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        String errorJson = null;
        try {
            errorJson = JsonUtil.objectToJson(new ErrorInfo(response.getStatus(), (String) request.getAttribute(Dispatcher.ERROR_MESSAGE)));
        } catch (JsonException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ByteBuffer buffer = baseRequest.getResponse().getHttpOutput().getBuffer();
        ByteBufferOutputStream out = new ByteBufferOutputStream(buffer);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(errorJson);
        writer.flush();
        writer.close();
    }

    /**
     * CustomErrorHandler
     */
    public CustomErrorHandler() {
        super();
    }
}
