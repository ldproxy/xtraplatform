/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.cfg;

import org.glassfish.jersey.server.ContainerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author zahnen
 */
public class QueryParamConnegFilter implements ContainerRequestFilter {

    private final Map<String, MediaType> mediaExtentions;

    public QueryParamConnegFilter() {
        this.mediaExtentions = new HashMap();
        this.mediaExtentions.put("json", MediaType.APPLICATION_JSON_TYPE);
        this.mediaExtentions.put("geojson", MediaType.APPLICATION_JSON_TYPE);
        this.mediaExtentions.put("html", MediaType.TEXT_HTML_TYPE);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

        if (containerRequestContext.getUriInfo().getQueryParameters().containsKey("f")) {
            String format = containerRequestContext.getUriInfo().getQueryParameters().getFirst("f");

            final MediaType accept = mediaExtentions.get(format);

            if (accept != null) {
                containerRequestContext.getHeaders().putSingle("Accept", accept.toString());
            }
        }
    }
}