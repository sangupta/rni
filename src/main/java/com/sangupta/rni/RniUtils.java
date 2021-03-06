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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.StringUtils;

public class RniUtils {

    protected static final String REQUEST_HEADER_FOR_PARAMS = "X-RNI-Params";

    public static String getMethodParams(Method method) {
        if(method == null) {
            return null;
        }
        
        Parameter[] params = method.getParameters();
        if(AssertUtils.isEmpty(params)) {
            return StringUtils.EMPTY_STRING;
        }
        
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(Parameter param : params) {
            Class<?> clazz = param.getType();
            
            if(!first) {
                builder.append(',');
            }
            builder.append(clazz.getName());
            
            first = false;
        }
        
        return builder.toString();
    }

    public static int count(String str, char c) {
        if(AssertUtils.isEmpty(str)) {
            return 0;
        }
        
        char[] array = str.toCharArray();
        int count = 0;
        for(char ch : array) {
            if(ch == c) {
                count++;
            }
        }
        
        return count;
    }
}