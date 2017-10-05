/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.cfg;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import java.util.HashMap;
import java.util.Map;
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
    /**
     * Create a filter with suffix to media type mappings.
     *
     * @param mediaExtentions the suffix to media type mappings.
     */
    /*public QueryParamConnegFilter(Map<String, MediaType> mediaExtentions) {
        if (mediaExtentions == null) {
            throw new IllegalArgumentException();
        }

        this.mediaExtentions = mediaExtentions;
        this.languageExtentions = Collections.emptyMap();
    }*/

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // Quick check for a 'f' parameter
        if (!request.getQueryParameters().containsKey("f")) {
            return request;
        }

        String format = request.getQueryParameters().getFirst("f");


        final MediaType accept = mediaExtentions.get(format);
        if (accept != null) {
            request.getRequestHeaders().putSingle("Accept", accept.toString());
        }

        return request;
    }
}