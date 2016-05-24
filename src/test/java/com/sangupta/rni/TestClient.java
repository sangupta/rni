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
import java.net.ServerSocket;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.constants.HttpStatusCode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);
    
    private static final int RANDOM_PORT = 0;

    private static Server JETTY_SERVER;
    
    private static int SERVER_PORT;
    
    private static final String SERVER_CONTEXT = "rni";
    
    private static final String SERVER_CONTEXT_URI_PIECE = "/" + SERVER_CONTEXT + "/";
    
    private static final RPCReceivingServlet SERVLET = new RPCReceivingServlet();
    
    static {
        try {
            ServerSocket socket = new ServerSocket(RANDOM_PORT);
            SERVER_PORT = socket.getLocalPort();

            // close the socket
            socket.close();
        } catch(Exception e) {
            
        }
    }
    
    private static PersonInterface PERSON_CLIENT_IMPL = WebClientGenerator.createWebClient(PersonInterface.class, "localhost", SERVER_PORT, SERVER_CONTEXT);
    
    private static PersonInterface PERSON_SERVER_IMPL = new PersonInterfaceImpl();

    @BeforeClass
    public static void setupJettyServer() throws IOException {
        // init logger
        TestRniLogger.initLogger();
        
        // start jetty server
        JETTY_SERVER = new Server();

        // multiple connectors
        ServerConnector serverConnector = new ServerConnector(JETTY_SERVER);
        serverConnector.setPort(SERVER_PORT);

        JETTY_SERVER.setConnectors(new Connector[] { serverConnector });

        // set up handlers
        JETTY_SERVER.setHandler(new JettyProxyToRniServlet());

        try {
            JETTY_SERVER.start();
        } catch (Exception e) {
            LOGGER.error("Unable to start jetty server", e);
            return;
        }
    }
    
    @AfterClass
    public static void shutdownJettyServer() {
        Server localReference = JETTY_SERVER;
        if(localReference != null) {
            if(!localReference.isStopped()) {
                try {
                    localReference.stop();
                } catch (Exception e) {
                    // eat up
                }
            }

            localReference.destroy();
        }
    }
    
    @Test
    public void test001InterfaceNotRegisteredOnServer() {
        Person person = new Person();
        person.name = "rni";
        person.age = 30;
        person.address = "github";
        
        // person must be created
        Assert.assertNull(PERSON_CLIENT_IMPL.createPerson(person));
        Assert.assertNull(PERSON_CLIENT_IMPL.getPerson("123"));
        Assert.assertNull(PERSON_CLIENT_IMPL.getPerson(person));
    }
    
    @Test
    public void test002InterfaceWhenRegisteredOnServer() {
        RPCReceivingServlet.recieveCalls(PersonInterface.class, PERSON_SERVER_IMPL);
        
        Person person = new Person();
        person.name = "rni";
        person.age = 30;
        person.address = "github";
        
        // person must be created
        String created = PERSON_CLIENT_IMPL.createPerson(person);
        Assert.assertNotNull(created);
        Assert.assertNotNull(PERSON_CLIENT_IMPL.getPerson(created));
        Assert.assertNull(PERSON_CLIENT_IMPL.getPerson(person));
        
        person.id = created;
        Assert.assertNotNull(PERSON_CLIENT_IMPL.getPerson(person));
        
        Map<String, String> map = PERSON_CLIENT_IMPL.getProperties();
        System.out.println(map);
    }

    private static class JettyProxyToRniServlet extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String uri = request.getRequestURI();
            
            if(!uri.startsWith(SERVER_CONTEXT_URI_PIECE)) {
                response.sendError(HttpStatusCode.NOT_FOUND);
                baseRequest.setHandled(true);
                return;
            }
            
            SERVLET.service(request, response);
            baseRequest.setHandled(true);
        }
        
    }
}