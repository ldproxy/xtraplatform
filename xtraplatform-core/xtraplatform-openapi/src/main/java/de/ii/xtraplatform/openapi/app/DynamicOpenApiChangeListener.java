package de.ii.xtraplatform.openapi.app;

import de.ii.xtraplatform.dropwizard.domain.JaxRsChangeListener;
import io.swagger.v3.core.filter.OpenAPISpecFilter;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public interface DynamicOpenApiChangeListener extends JaxRsChangeListener {

  @Override
  void jaxRsChanged();

  Response getOpenApi(
      HttpHeaders headers, UriInfo uriInfo, String type, OpenAPISpecFilter specFilter)
      throws Exception;

  Response getOpenApi(HttpHeaders headers, ServletConfig config, Application app, UriInfo uriInfo, String yaml)
      throws Exception;
}
