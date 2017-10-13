package de.ii.xtraplatform.openapi;


/**
 * @author zahnen
 */


import io.swagger.oas.annotations.Operation;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Component
@Provides(specifications = {DynamicOpenApiResource.class})
@Instantiate
@Path("/openapi.{type:json|yaml}")
public class DynamicOpenApiResource {

    private static Logger LOGGER = LoggerFactory.getLogger(DynamicOpenApiResource.class);

    @Requires
    DynamicOpenApi openApi;

    @Context
    ServletConfig config;

    @Context
    Application app;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(summary = "Returns the OpenAPI spec")
    public Response getOpenApi( @Context HttpHeaders headers,
                                @Context UriInfo uriInfo,
                                @PathParam("type") String type) throws Exception {

        return openApi.getOpenApi(headers, config, app, uriInfo, type);
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
