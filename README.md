# rni

`rni` or **RPC Negotiable Interfaces** is a library to massively speed up the development of distributed
applications in Java. It exposes any standard **Java Interface** over REST webservices to be consumed and
allows the developer to focus more on business functionality.

**NOTE**: The library is meant to be used during early development cycles and then replaces with something
better as in `Apache Thrift` or otherwise.

## Usage

Say you have an interface `PersonInterface.java` as:

```java
public interface PersonInterface {
    
    public Person getPerson(String id);
    
    public String getPerson(Person person);

    public String createPerson(Person person);
    
}
```

Say we have an implementating class for the same as `PersonInterfaceImpl.java`:

```java
public class PersonInterfaceImpl implements PersonInterface {

    // implementation methods

}
```

Generate a `webservice` using `HttpServlet` as:

```java
// register the interface to receive calls
RPCReceivingServlet.recieveCalls(PersonInterface.class, new PersonInterfaceImpl());
```

```xml
<servlet>
    <description>Handles RNI calls</description>
    <display-name>RNI-Servlet</display-name>
    <servlet-name>rni</servlet-name>
    <servlet-class>com.sangupta.rni.RPCReceivingServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>rni</servlet-name>
    <url-pattern>/rni/*</url-pattern>
</servlet-mapping>
```

To make calls from a second application (aka the client application):

```java
PersonInterface clientProxy =  WebClientGenerator.createWebClient(PersonInterface.class, "localhost", 8080, "/rni/");

// make regular calls like
Person person = clientProxy.getPerson("person-id-123");
```

## License

```
neo - project scaffolding tool
Copyright (c) 2016, Sandeep Gupta

http://sangupta.com/projects/neo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
