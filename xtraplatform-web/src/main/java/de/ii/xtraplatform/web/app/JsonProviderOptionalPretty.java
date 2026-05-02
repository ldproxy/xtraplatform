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
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.fasterxml.jackson.jakarta.rs.json.JsonEndpointConfig;
import de.ii.xtraplatform.web.domain.JsonPretty;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Custom JSON reader and writer that supports optional pretty JSON output via request context
 * property
 */
@Provider
@Consumes({"*/*"})
@Produces({"application/json", "text/json", "*/*"})
@Priority(Priorities.ENTITY_CODER)
public class JsonProviderOptionalPretty extends JacksonXmlBindJsonProvider
    implements WriterInterceptor {

  private final ObjectMapper mapper;
  private final ObjectMapper mapperPretty;

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
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
