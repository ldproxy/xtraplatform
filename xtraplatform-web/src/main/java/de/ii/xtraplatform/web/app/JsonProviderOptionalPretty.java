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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * Custom JSON reader and writer that supports optional pretty JSON output via request context
 * property
 */
@Provider
@Consumes({"*/*"})
@Produces({"application/json", "text/json", "*/*"})
@Priority(Priorities.ENTITY_CODER)
public class JsonProviderOptionalPretty extends JacksonJaxbJsonProvider
    implements WriterInterceptor {

  private final ObjectMapper mapper;
  private final ObjectMapper mapperPretty;

  public JsonProviderOptionalPretty(ObjectMapper mapper) {
    super();
    this.mapper = mapper;
    this.mapperPretty = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);

    setMapper(mapper);
  }

  @Override
  public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException {
    // check if the request context has the JSON pretty property set
    if (JsonPretty.isJsonPretty(writerInterceptorContext)) {

      // if so, add the JsonPrettify annotation to the writer context, since the method that selects
      // the object mapper for writing cannot access the request context
      JsonPretty.addAnnotation(writerInterceptorContext);
    }

    writerInterceptorContext.proceed();
  }

  @Override
  protected JsonEndpointConfig _endpointForWriting(
      Object value,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders) {
    boolean pretty = JsonPretty.isJsonPretty(annotations);

    return this._configForWriting(
        pretty ? mapperPretty : mapper, annotations, this._defaultWriteView);
  }
}
