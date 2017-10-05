/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
