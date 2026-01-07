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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import io.dropwizard.util.DataSize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.LoaderOptions;

public class ValueEncodingJackson<T> implements ValueEncoding<T> {

  public static final byte[] YAML_NULL = "--- null\n".getBytes();
  private static final FORMAT DEFAULT_FORMAT = FORMAT.YML;

  private final Map<FORMAT, ObjectMapper> mappers;
  private final List<ValueDecoderMiddleware<byte[]>> decoderPreProcessor;
  private final List<ValueDecoderMiddleware<T>> decoderMiddleware;
  private final JacksonHelper<T> jacksonHelper;

  public ValueEncodingJackson(
      Jackson jackson, DataSize maxYamlFileSize, boolean failOnUnknownProperties) {
    DataSize resolvedMaxYamlFileSize =
        Objects.requireNonNullElse(maxYamlFileSize, DataSize.megabytes(3));

    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setCodePointLimit(Math.toIntExact(resolvedMaxYamlFileSize.toBytes()));

    ObjectMapper jsonMapper =
        jackson
            .getDefaultObjectMapper()
            .copy()
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setDefaultMergeable(true);

    ObjectMapper yamlMapper =
        jackson
            .getNewObjectMapper(
                YAMLFactory.builder()
                    .loaderOptions(loaderOptions)
                    .disable(Feature.USE_NATIVE_TYPE_ID)
                    .disable(Feature.USE_NATIVE_OBJECT_ID)
                    .disable(Feature.INDENT_ARRAYS)
                    .disable(Feature.USE_PLATFORM_LINE_BREAKS)
                    .disable(Feature.SPLIT_LINES)
                    .disable(Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                    .enable(Feature.WRITE_DOC_START_MARKER)
                    .enable(Feature.MINIMIZE_QUOTES)
                    .build())
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setDefaultMergeable(true)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    ObjectMapper smileMapper =
        jackson
            .getNewObjectMapper(new SmileFactory())
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setDefaultMergeable(true)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    if (failOnUnknownProperties) {
      jsonMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      yamlMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    this.mappers =
        ImmutableMap.of(
            FORMAT.JSON, jsonMapper,
            FORMAT.YML, yamlMapper,
            FORMAT.YAML, yamlMapper,
            FORMAT.SMILE, smileMapper);

    this.decoderMiddleware = new ArrayList<>();
    this.decoderPreProcessor = new ArrayList<>();
    this.jacksonHelper =
        new JacksonHelper<>(
            decoderPreProcessor, decoderMiddleware, resolvedMaxYamlFileSize, this::getMapper);
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

  @Override
  public final byte[] serialize(T data) {
    return serialize(data, DEFAULT_FORMAT);
  }

  @Override
  public final byte[] serialize(T data, FORMAT format) {
    return jacksonHelper.serialize(data, format);
  }

  @Override
  public final T deserialize(
      Identifier identifier, byte[] payload, FORMAT format, boolean ignoreCache)
      throws IOException {
    return jacksonHelper.deserialize(identifier, payload, format, ignoreCache);
  }

  @Override
  public final ObjectMapper getMapper(FORMAT format) {
    return mappers.get(format);
  }

  public final boolean isEmpty(byte[] payload) {
    return jacksonHelper.isEmpty(payload);
  }

  @Override
  public byte[] serialize(Map<String, Object> data) {
    return jacksonHelper.serialize(data, DEFAULT_FORMAT);
  }

  @Override
  public final String hash(T data) {
    return jacksonHelper.hash(data);
  }
}
