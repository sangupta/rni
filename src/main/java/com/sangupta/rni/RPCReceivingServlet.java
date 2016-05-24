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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sangupta.jerry.constants.HttpMimeType;
import com.sangupta.jerry.constants.HttpStatusCode;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.GsonUtils;
import com.sangupta.jerry.util.ResponseUtils;

public class RPCReceivingServlet extends HttpServlet {
    
    /**
     * Generated via Eclipse
     */
    private static final long serialVersionUID = 8455871794617981601L;

    /**
     * My logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RPCReceivingServlet.class);
    
    /**
     * Holds all the currently valid end-points from all interfaces mapped to this
     * class. The mapping is of the form: /class.getSimpleName()/class.getMethod().getName()
     */
    private static final Map<String, Map<String, MappedInvocationMethod>> END_POINTS = new HashMap<>();

    /**
     * Register a new interface to be supported to receive calls.
     * 
     * @param classOfT the interface for which calls are to be supported
     * 
     * @param instance the actual implementation to use for working through RPC calls
     * 
     * @return
     */
    public static <T> boolean recieveCalls(Class<T> classOfT, T instance) {
        if(classOfT == null) {
            throw new IllegalArgumentException("ClassOfT cannot be null");
        }
        
        if(instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        
        // scan the interface and find all public methods
        final String className = classOfT.getSimpleName();
        Method[] methods = classOfT.getMethods();
        if(AssertUtils.isEmpty(methods)) {
            LOGGER.info("No methods in the defined interface... nothing to receive");
            return false;
        }
        
        for(Method method : methods) {
            final String methodName = method.getName();
            if(!Modifier.isPublic(method.getModifiers())) {
                LOGGER.debug("Skipping method as is not public: {}@{}", methodName, className);
                continue;
            }
            
            if(!Modifier.isAbstract(method.getModifiers())) {
                LOGGER.debug("Skipping method as is abstract: {}@{}", methodName, className);
                continue;
            }
            
            final String methodParams = RniUtils.getMethodParams(method);
            
            LOGGER.debug("Adding method for receiving calls: {}({})", methodName, methodParams);
            
            String key = className + "/" + methodName;
            
            if(END_POINTS.containsKey(key)) {
                END_POINTS.get(key).put(methodParams, new MappedInvocationMethod(instance, method, methodParams));
            } else {
                Map<String, MappedInvocationMethod> map = new HashMap<>();
                map.put(methodParams, new MappedInvocationMethod(instance, method, methodParams));
                END_POINTS.put(key, map);
            }
        }
        
        return true;
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String uri = extractUri(request);

        // check for rni header
        final String methodParams = request.getHeader(RniUtils.REQUEST_HEADER_FOR_PARAMS);
        if(methodParams == null) {
            LOGGER.debug("No RNI header present: {}", uri);
            response.sendError(HttpStatusCode.BAD_REQUEST);
            return;
        }
        
        // extract method name
        Map<String, MappedInvocationMethod> map = END_POINTS.get(uri);
        if(map == null) {
            LOGGER.debug("End point not mapped to any instance: {}", uri);
            response.sendError(HttpStatusCode.NOT_FOUND);
            return;
        }
        
        // find the right method to be invoked
        MappedInvocationMethod mappedMethod = map.get(methodParams);
        if(mappedMethod == null) {
            LOGGER.debug("End point not initialized to any instance: {}", uri);
            response.sendError(HttpStatusCode.INTERNAL_SERVER_ERROR);
            return;
        }
        
        final int numArguments = RniUtils.count(methodParams, ',') + 1;
        final String[] classArray = methodParams.split(",");
        
        // find number and type of arguments that have been sent in the payload
        byte[] bytes = IOUtils.toByteArray(request.getInputStream());
        String json = new String(bytes, Charsets.UTF_8);
        
        // parse the body
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(json);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        
        // make a call to this method
        Set<Entry<String, JsonElement>> set = jsonObject.entrySet();
        Object[] args = new Object[set.size()];
        Class<?>[] params = new Class[set.size()];
        
        for(int index = 0; index < numArguments; index++) {
            JsonElement element = jsonObject.get("param-" + index);
            
            Class<?> clazz = null;
            try {
                clazz = Class.forName(classArray[index]);
            } catch (ClassNotFoundException e) {
                response.sendError(HttpStatusCode.INTERNAL_SERVER_ERROR);
                return;
            }
            
            params[index] = clazz;
            args[index++] = GsonUtils.getGson().fromJson(element, clazz);
        }
        
        // invoke the method
        try {
            Object result = mappedMethod.method.invoke(mappedMethod.instance, args);
            
            if(result == null) {
                response.setStatus(HttpStatusCode.NO_CONTENT);
                return;
            }
            
            ResponseUtils.sendResponse(response, GsonUtils.getGson().toJson(result), HttpMimeType.JSON);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            response.sendError(HttpStatusCode.INTERNAL_SERVER_ERROR);
            return;        
        }
    }
    
    public static String extractUri(HttpServletRequest request) {
        // extract the URL
        String url = request.getRequestURI();
        if(url.startsWith("/rni")) {
            url = url.substring(4);
        }
        
        int jsessionID = url.indexOf(";jsessionid=");
        if(jsessionID != -1) {
            url = url.substring(0, jsessionID);
        }

        if(url.startsWith("/")) {
            url = url.substring(1);
        }
        
        return url;
    }
    
    private static class MappedInvocationMethod {
        
        /**
         * The object instance over which the method will be invoked
         */
        final Object instance;
        
        /**
         * Method that needs to be invoked
         */
        final Method method;
        
        final String methodParams;
        
        public MappedInvocationMethod(Object instance, Method method, String methodParams) {
            this.instance = instance;
            this.method = method;
            this.methodParams = methodParams;
        }

    }

}