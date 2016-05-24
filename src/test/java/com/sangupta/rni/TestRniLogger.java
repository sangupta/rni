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

import org.slf4j.LoggerFactory;

import com.sangupta.jerry.util.AssertUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Logback based logger that is configured programmatically.
 *
 * @author sangupta
 *
 */
public class TestRniLogger {

	private static ConsoleAppender<ILoggingEvent> consoleAppender;

	private static boolean loggerInitialized = false;

    public static Logger addLogger(String loggingClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        return addLogger(logger, Level.DEBUG);
    }

    public static Logger addLogger(String loggingClass, Level logLevel) {
    	if(AssertUtils.isEmpty(loggingClass)) {
    		return null;
    	}

    	Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
    	return addLogger(logger, logLevel);
    }

    private static Logger addLogger(Logger logger, Level logLevel) {
    	logger.setLevel(logLevel);
        logger.setAdditive(false);

        return logger;
	}

	public static synchronized void initLogger() {
		if(loggerInitialized) {
			return;
		}

		loggerInitialized = true;

		final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		// go ahead and create logger
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("%d %-5level [%-15thread] [%logger] - %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.setOutputPatternAsHeader(true);
        patternLayoutEncoder.start();

        // the console appender
		consoleAppender = new ConsoleAppender<>();
        consoleAppender.setEncoder(patternLayoutEncoder);
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("console");
        consoleAppender.start();

        // the root logger
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
        addLogger(rootLogger, Level.DEBUG);

        addLogger("com.sangupta.rni", Level.DEBUG);
        addLogger("org.springframework", Level.WARN);
        addLogger("org.eclipse.jetty", Level.WARN);
	}

}