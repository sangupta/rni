/**
 *
 * rni - rpc negotiable interfaces
 * Copyright (c) 2016, Sandeep Gupta
 * 
 * http://sangupta.com/projects/rni
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.sangupta.rni;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.constants.HttpMimeType;
import com.sangupta.jerry.http.WebInvoker;
import com.sangupta.jerry.http.WebRequest;
import com.sangupta.jerry.http.WebResponse;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.GsonUtils;
import com.sangupta.jerry.util.UriUtils;
import com.sangupta.jerry.util.UrlManipulator;

public class WebClientGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientGenerator.class);

    public static <T> T createWebClient(final Class<T> classOfT, final String host, final int port, final String context) {
        Object instance = Proxy.newProxyInstance(classOfT.getClassLoader(), new Class[] { classOfT }, new InvocationHandler() {
            
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                Class<?>[] classes = method.getParameterTypes();
                
                UrlManipulator urlManipulator = new UrlManipulator(host, port, context);
                String url = urlManipulator.constructURL();
                url = UriUtils.addWebPaths(url, classOfT.getSimpleName(), methodName);
                
                LOGGER.debug("Invoking the web-service at URL: {}", url);
                
                // let's make the request
                WebRequest request;
                if(AssertUtils.isEmpty(classes)) {
                    // GET request
                    request = WebRequest.get(url);
                } else {
                    // POST request
                    request = WebRequest.post(url);
                }
                
                request.addHeader(RniUtils.REQUEST_HEADER_FOR_PARAMS, RniUtils.getMethodParams(method));
                
                // add the params body if needed
                if(AssertUtils.isNotEmpty(classes)) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    for(int index = 0; index < classes.length; index++) {
                        map.put("param-" + index, args[index]);
                    }
                    
                    String json = GsonUtils.getGson().toJson(map);
                    
                    LOGGER.debug("JSON post body for parameters: {}", json);
                    request.bodyString(json, HttpMimeType.JSON, "utf-8");
                }
                
                // TODO: remove timing thing
                final long start = System.currentTimeMillis();
                WebResponse response = WebInvoker.executeSilently(request);
                final long end = System.currentTimeMillis();
                
                System.out.println("Call to uri took " + (end - start) + " millis: " + url);
                
                if(response == null) {
                    return null;
                }
                
                if(!response.isSuccess()) {
                    return null;
                }
                
                // get return type of method
                Class<?> returnType = method.getReturnType();
                String responseBody = response.getContent();
                return GsonUtils.getGson().fromJson(responseBody, returnType);
            }

        });
        
        return classOfT.cast(instance);
    }
    
}