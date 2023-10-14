/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.api;

import static de.ii.xtraplatform.base.domain.util.JacksonModules.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make default format and supported formats configurable
public class ValueEncodingJackson<T> implements ValueEncoding<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueEncodingJackson.class);

  public static final byte[] JSON_NULL = "null".getBytes();
  public static final byte[] YAML_NULL = "--- null\n".getBytes();
  private static final Pattern JSON_EMPTY = Pattern.compile("(\\s)*");
  private static final Pattern YAML_EMPTY = Pattern.compile("---(\\s)*");

  private static final FORMAT DEFAULT_FORMAT = FORMAT.YML;

  @Deprecated
  private static final FORMAT DESER_FORMAT_LEGACY =
      FORMAT.JSON; // old configuration files without file extension are JSON

  private final Map<FORMAT, ObjectMapper>
      mappers; // TODO: use smile/ion mapper for distributed store
  private final List<ValueDecoderMiddleware<byte[]>> decoderPreProcessor;
  private final List<ValueDecoderMiddleware<T>> decoderMiddleware;

  public ValueEncodingJackson(Jackson jackson, boolean failOnUnknownProperties) {

    ObjectMapper jsonMapper =
        jackson
            .getDefaultObjectMapper()
            .copy()
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setDefaultMergeable(true);

    ObjectMapper yamlMapper =
        jackson
            .getNewObjectMapper(
                new YAMLFactory()
                    .disable(Feature.USE_NATIVE_TYPE_ID)
                    .disable(Feature.USE_NATIVE_OBJECT_ID)
                    .disable(Feature.INDENT_ARRAYS)
                    .disable(Feature.USE_PLATFORM_LINE_BREAKS)
                    .disable(Feature.SPLIT_LINES)
                    .disable(Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                    .enable(Feature.WRITE_DOC_START_MARKER)
                    .enable(Feature.MINIMIZE_QUOTES))
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setDefaultMergeable(true)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    if (failOnUnknownProperties) {
      jsonMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      yamlMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    this.mappers =
        ImmutableMap.of(
            FORMAT.JSON, jsonMapper,
            FORMAT.YML, yamlMapper,
            FORMAT.YAML, yamlMapper);

    this.decoderMiddleware = new ArrayList<>();
    this.decoderPreProcessor = new ArrayList<>();
  }

  public void addDecoderPreProcessor(ValueDecoderMiddleware<byte[]> preProcessor) {
    this.decoderPreProcessor.add(preProcessor);
  }

  public void addDecoderMiddleware(ValueDecoderMiddleware<T> middleware) {
    this.decoderMiddleware.add(middleware);
  }

  @Override
  public final FORMAT getDefaultFormat() {
    return DEFAULT_FORMAT;
  }

  // TODO: default serialization format should depend on EventStore implementation
  @Override
  public final byte[] serialize(T data) {
    try {
      return getDefaultMapper().writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      // should never happen
      throw new IllegalStateException("Unexpected serialization error", e);
    }
  }

  @Override
  public byte[] serialize(Map<String, Object> data) {
    try {
      return getDefaultMapper().writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      // should never happen
      throw new IllegalStateException("Unexpected serialization error", e);
    }
  }

  @Override
  public final T deserialize(
      Identifier identifier, byte[] payload, FORMAT format, boolean ignoreCache)
      throws IOException {
    // "null" as payload means delete
    if (isNull(payload)) {
      return null;
    }

    byte[] rawData = payload;
    FORMAT payloadFormat = Objects.equals(format, FORMAT.NONE) ? DESER_FORMAT_LEGACY : format;
    ObjectMapper objectMapper = getMapper(payloadFormat);
    T data = null;

    for (ValueDecoderMiddleware<byte[]> preProcessor : decoderPreProcessor) {
      rawData = preProcessor.process(identifier, rawData, objectMapper, null, ignoreCache);
    }

    try {
      for (ValueDecoderMiddleware<T> middleware : decoderMiddleware) {
        data = middleware.process(identifier, rawData, objectMapper, data, ignoreCache);
      }

    } catch (Throwable e) {
      Optional<ValueDecoderMiddleware<T>> recovery =
          decoderMiddleware.stream().filter(ValueDecoderMiddleware::canRecover).findFirst();
      if (recovery.isPresent()) {
        try {
          data = recovery.get().recover(identifier, rawData, objectMapper);
        } catch (Throwable e2) {
          throw e;
        }
      } else {
        throw e;
      }
    }

    return data;
  }

  final ObjectMapper getDefaultMapper() {
    return getMapper(DEFAULT_FORMAT);
  }

  @Override
  public final ObjectMapper getMapper(FORMAT format) {
    return mappers.get(format);
  }

  final boolean isNull(byte[] payload) {
    return Arrays.equals(payload, JSON_NULL) || Arrays.equals(payload, YAML_NULL);
  }

  public final boolean isEmpty(byte[] payload) {
    String payloadString = new String(payload, StandardCharsets.UTF_8);
    return JSON_EMPTY.matcher(payloadString).matches()
        || YAML_EMPTY.matcher(payloadString).matches();
  }
}
