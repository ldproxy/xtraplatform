/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.openapi;


/**
 * @author zahnen
 */


import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import io.swagger.oas.annotations.enums.Explode;
import io.swagger.oas.annotations.media.Content;
import io.swagger.oas.annotations.media.Schema;
import io.swagger.oas.annotations.responses.ApiResponse;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Component
@Provides(specifications = {DynamicOpenApiResource.class})
@Instantiate
@Path("/api")
public class DynamicOpenApiResource {

    private static Logger LOGGER = LoggerFactory.getLogger(DynamicOpenApiResource.class);

    @Requires
    DynamicOpenApi openApi;

    @Requires(optional = true)
    OpenApiViewerResource openApiViewerResource;

    @Context
    ServletConfig config;

    @Context
    Application app;

    @GET
    @Produces({MediaType.TEXT_HTML})
    @Operation(
            summary = "the API description - this document",
            tags = {"Capabilities"},
            parameters = {@Parameter(name = "f")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The formal documentation of this API according to the OpenAPI specification, version 3.0. I.e., this document.",
                            content = {
                                    @Content(
                                            mediaType = "application/openapi+json;version=3.0",
                                            schema = @Schema(type = "object")
                                    ),
                                    @Content(
                                            mediaType = "text/html",
                                            schema = @Schema(type = "string")
                                    )
                            }
                    ),
                    @ApiResponse(
                            description = "An error occured.",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(ref = "#/components/schemas/exception")
                                    ),
                                    @Content(
                                            mediaType = "application/xml",
                                            schema = @Schema(ref = "#/components/schemas/exception")
                                    ),
                                    @Content(
                                            mediaType = "text/html",
                                            schema = @Schema(type = "string")
                                    )
                            }
                    )
            })
    public Response getApiDescription(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {} {}", "HTML", headers.getHeaderString("Accept"));
        return openApiViewerResource.getFile("index.html");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionJson(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {})", "JSON");
        return openApi.getOpenApi(headers, config, app, uriInfo, "json");
    }


    @GET
    @Produces({DynamicOpenApi.YAML})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionYaml(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {})", "YAML");
        return openApi.getOpenApi(headers, config, app, uriInfo, "yaml");
    }

    @GET
    @Path("/{file}")
    @CacheControl(maxAge = 3600)
    public Response getFile(@PathParam("file") String file) {
        LOGGER.debug("FILE {})", file);

        if (openApiViewerResource == null) {
            throw new NotFoundException();
        }

        return openApiViewerResource.getFile(file);
    }


    /*@GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    //@ApiOperation(value = "The openapi definition in either JSON or YAML", hidden = true)
    //@Operation
    public Response getListing(
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @PathParam("type") String type) {
        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            return null;//openApi.getListingYamlResponse(headers, uriInfo);
        } else {
            return null;//openApi.getListingJsonResponse(headers, uriInfo);
        }
    }*/

}
