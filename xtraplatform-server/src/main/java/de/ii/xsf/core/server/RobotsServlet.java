/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.server;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author zahnen
 */

@Component
@Provides(properties = {
        @StaticServiceProperty(name = "alias", type = "java.lang.String", value = "/robots.txt")
})
@Instantiate

public class RobotsServlet extends HttpServlet implements ContainerResponseFilter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(RobotsServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("text/plain");
        writeContent(response.getOutputStream());
        response.getOutputStream().close();
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        if (request.getPath().endsWith("services/robots.txt")) {
            response.reset();

            StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    writeContent(output);
                }
            };

            Response r = Response.ok(stream, "text/plain").build();
            response.setResponse(r);
        }

        return response;
    }

    private void writeContent(OutputStream output) {
        PrintStream printStream = new PrintStream(output);
        printStream.println("User-agent: *");
        //printStream.println("Disallow: /cgi-bin/");
        printStream.close();
    }
}
