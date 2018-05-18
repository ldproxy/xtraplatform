/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.openapi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xsf.core.web.JaxRsChangeListener;
import de.ii.xsf.core.web.JaxRsReg;
import io.swagger.core.filter.OpenAPISpecFilter;
import io.swagger.core.filter.SpecFilter;
import io.swagger.jaxrs2.Reader;
import io.swagger.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.*;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.QueryParameter;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.tags.Tag;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Provides(specifications = {DynamicOpenApi.class})
@Instantiate
public class DynamicOpenApi extends BaseOpenApiResource implements JaxRsChangeListener {

    private static Logger LOGGER = LoggerFactory.getLogger(DynamicOpenApi.class);
    public static final MediaType YAML_TYPE = new MediaType("application", "yaml");
    public final static String YAML = "application/yaml";
    @Requires
    private JaxRsReg registry;

    private OpenAPI openApiSpec;
    private boolean upToDate;

    @Validate
    private void start() {
        registry.addChangeListener(this);
    }

    private synchronized void scan() {
        Reader reader = new Reader(new OpenAPI());
        this.openApiSpec = reader.read(getResourceClasses());
        this.upToDate = true;
    }

    private synchronized Set<Class<?>> getResourceClasses() {
        return registry.getResources()
                .stream()
                .map(Object::getClass)
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized void jaxRsChanged() {
        this.upToDate = false;
    }

    @Override
    protected Response getOpenApi(HttpHeaders headers, ServletConfig config, Application app, UriInfo uriInfo, String type) throws Exception {
        return getOpenApi(headers, uriInfo, type, null);
    }

    public Response getOpenApi(HttpHeaders headers, UriInfo uriInfo, String type, OpenAPISpecFilter specFilter) throws Exception {

        synchronized (DynamicOpenApi.class) {
            if (!upToDate) {
                scan();
            }
        }

        if (openApiSpec == null) {
            return Response.status(404).build();
        }

        OpenAPI oas = openApiSpec;
        boolean pretty = true;

        if (specFilter != null) {
            SpecFilter f = new SpecFilter();
            oas = f.filter(openApiSpec, specFilter, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers), getHeaders(headers));
        }

        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            return Response.status(Response.Status.OK)
                    .entity(pretty ? Yaml.pretty(oas) : Yaml.mapper().writeValueAsString(oas))
                    .type("application/yaml")
                    .build();
        } else {
            return Response.status(Response.Status.OK)
                    .entity(pretty ? Json.pretty(oas) : Json.mapper().writeValueAsString(oas))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (params != null) {
            for (String key : params.keySet()) {
                List<String> values = params.get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    private static Map<String, String> getCookies(HttpHeaders headers) {
        Map<String, String> output = new HashMap<String, String>();
        if (headers != null) {
            for (String key : headers.getCookies().keySet()) {
                Cookie cookie = headers.getCookies().get(key);
                output.put(key, cookie.getValue());
            }
        }
        return output;
    }

    private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (headers != null) {
            for (String key : headers.getRequestHeaders().keySet()) {
                List<String> values = headers.getRequestHeaders().get(key);
                output.put(key, values);
            }
        }
        return output;
    }
}

/**
 * @author zahnen
 */

/*
import de.ii.xsf.core.web.rest.JaxRsChangeListener;
import de.ii.xsf.core.web.rest.JaxRsReg;
import io.openapi.annotations.Info;
import io.openapi.annotations.SwaggerDefinition;
import io.openapi.config.FilterFactory;
import io.openapi.core.filter.SpecFilter;
import io.openapi.core.filter.SwaggerSpecFilter;
import io.openapi.jaxrs.Reader;
import io.openapi.jaxrs.listing.SwaggerSerializers;
import io.openapi.models.Swagger;
import io.openapi.util.Yaml;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.*;
import java.util.*;

@SwaggerDefinition(
        info = @Info(
                description = "XtraPlatform Admin API",
                version = "1.0.0",
                title = "XtraPlatform Admin API"
        ),
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
@Component
@Provides(specifications = {SwaggerApi.class})
@Instantiate
public class SwaggerApi implements JaxRsChangeListener {


    @Requires
    private JaxRsReg registry;

    private Swagger openapi;
    private boolean upToDate;

    public SwaggerApi() {

    }

    @Validate
    private void start() {
        SwaggerSerializers.setPrettyPrint(true);
        registry.addService(new SwaggerSerializers());
        registry.addChangeListener(this);
    }

    private synchronized void scan() {
        this.openapi = new Swagger();
        Reader reader = new Reader(openapi);
        openapi = reader.read(getResourceClasses());
        upToDate = true;
    }

    private synchronized Set<Class<?>> getResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(this.getClass());
        for (Object resource : registry.getResources()) {
            classes.add(resource.getClass());
        }
        return classes;
    }

    private Swagger process(
            HttpHeaders headers,
            UriInfo uriInfo) {
        synchronized (SwaggerApiListingResource.class) {
            if (!upToDate) {
                scan();
            }
        }
        if (openapi != null) {
            SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
            if (filterImpl != null) {
                SpecFilter f = new SpecFilter();
                openapi = f.filter(openapi, filterImpl, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers),
                        getHeaders(headers));
            }
        }
        return openapi;
    }

    public Response getListingYamlResponse(
            HttpHeaders headers,
            UriInfo uriInfo) {
        Swagger openapi = process(headers, uriInfo);
        try {
            if (openapi != null) {
                String yaml = Yaml.mapper().writeValueAsString(openapi);
                StringBuilder b = new StringBuilder();
                String[] parts = yaml.split("\n");
                for (String part : parts) {
                    b.append(part);
                    b.append("\n");
                }
                return Response.ok().entity(b.toString()).type("application/yaml").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.status(404).build();
    }

    public Response getListingJsonResponse(
           HttpHeaders headers,
            UriInfo uriInfo) {
        Swagger openapi = process(headers, uriInfo);

        if (openapi != null) {
            return Response.ok().entity(openapi).build();
        } else {
            return Response.status(404).build();
        }
    }

    private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (params != null) {
            for (String key : params.keySet()) {
                List<String> values = params.get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    private static Map<String, String> getCookies(HttpHeaders headers) {
        Map<String, String> output = new HashMap<String, String>();
        if (headers != null) {
            for (String key : headers.getCookies().keySet()) {
                Cookie cookie = headers.getCookies().get(key);
                output.put(key, cookie.getValue());
            }
        }
        return output;
    }

    private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (headers != null) {
            for (String key : headers.getRequestHeaders().keySet()) {
                List<String> values = headers.getRequestHeaders().get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    @Override
    public synchronized void jaxRsChanged() {
        this.upToDate = false;
    }
}*/
