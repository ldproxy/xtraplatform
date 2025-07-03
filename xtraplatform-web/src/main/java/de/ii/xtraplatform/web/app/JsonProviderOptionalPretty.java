/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JsonEndpointConfig;
import de.ii.xtraplatform.web.domain.JsonPretty;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/** Custom JSON reader and writer that supports optional pretty JSON output via header */
@Provider
@Consumes({"*/*"})
@Produces({"application/json", "text/json", "*/*"})
@Priority(Priorities.ENTITY_CODER)
public class JsonProviderOptionalPretty extends JacksonJaxbJsonProvider
    implements ContainerResponseFilter {

  private final ObjectMapper mapper;
  private final ObjectMapper mapperPretty;

  public JsonProviderOptionalPretty(ObjectMapper mapper) {
    this.mapper = mapper;
    this.mapperPretty = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);

    setMapper(mapper);
  }

  /**
   * Filter method to add the JSON pretty header to the response if it exists in the request. The
   * MessageBodyWriter can only access the response headers. It will handle the actual pretty
   * printing based on this header.
   */
  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    if (requestContext.getHeaders().containsKey(JsonPretty.JSON_PRETTY_HEADER)) {
      responseContext
          .getHeaders()
          .add(
              JsonPretty.JSON_PRETTY_HEADER,
              requestContext.getHeaders().getFirst(JsonPretty.JSON_PRETTY_HEADER));
    }
  }

  @Override
  protected JsonEndpointConfig _endpointForWriting(
      Object value,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders) {
    boolean pretty = JsonPretty.isJsonPretty(httpHeaders);

    JsonPretty.cleanup(httpHeaders);

    return this._configForWriting(
        pretty ? mapperPretty : mapper, annotations, this._defaultWriteView);
  }
}
