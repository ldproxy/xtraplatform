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