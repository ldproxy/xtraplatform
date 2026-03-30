/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException;
import com.google.common.hash.Hashing;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import io.dropwizard.util.DataSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

class JacksonHelper<T> {

  private static final byte[] JSON_NULL = "null".getBytes();
  private static final byte[] YAML_NULL = "--- null\n".getBytes();
  private static final Pattern JSON_EMPTY = Pattern.compile("(\\s)*");
  private static final Pattern YAML_EMPTY = Pattern.compile("---(\\s)*");

  private final List<ValueDecoderMiddleware<byte[]>> decoderPreProcessor;
  private final List<ValueDecoderMiddleware<T>> decoderMiddleware;
  private final DataSize maxYamlFileSize;
  private final Function<FORMAT, ObjectMapper> mapperProvider;

  JacksonHelper(
      List<ValueDecoderMiddleware<byte[]>> decoderPreProcessor,
      List<ValueDecoderMiddleware<T>> decoderMiddleware,
      DataSize maxYamlFileSize,
      Function<FORMAT, ObjectMapper> mapperProvider) {
    this.decoderPreProcessor = decoderPreProcessor;
    this.decoderMiddleware = decoderMiddleware;
    this.maxYamlFileSize = maxYamlFileSize;
    this.mapperProvider = mapperProvider;
  }

  // Serialization methods
  byte[] serialize(Object data, FORMAT format) {
    try {
      return mapperProvider.apply(format).writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unexpected serialization error", e);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  String hash(Object data) {
    byte[] bytes = serialize(data, FORMAT.SMILE);
    return Hashing.murmur3_128().hashBytes(bytes).toString();
  }

  // Deserialization methods

  T deserialize(Identifier identifier, byte[] payload, FORMAT format, boolean ignoreCache)
      throws IOException {
    if (isNull(payload)) {
      return null;
    }
    if (Objects.equals(format, FORMAT.NONE)) {
      throw new IllegalStateException("No format given");
    }

    // Preprocess payload
    byte[] rawData = payload;
    ObjectMapper objectMapper = mapperProvider.apply(format);
    for (ValueDecoderMiddleware<byte[]> preProcessor : decoderPreProcessor) {
      rawData = preProcessor.process(identifier, rawData, objectMapper, null, ignoreCache);
    }

    return processMiddleware(identifier, rawData, objectMapper, ignoreCache);
  }

  boolean isEmpty(byte[] payload) {
    if (isNull(payload)) {
      return true;
    }
    String payloadString = new String(payload, StandardCharsets.UTF_8);
    return JSON_EMPTY.matcher(payloadString).matches()
        || YAML_EMPTY.matcher(payloadString).matches();
  }

  boolean isNull(byte[] payload) {
    return Arrays.equals(payload, JSON_NULL) || Arrays.equals(payload, YAML_NULL);
  }

  private T processMiddleware(
      Identifier identifier, byte[] rawData, ObjectMapper objectMapper, boolean ignoreCache)
      throws IOException {
    T data = null;

    try {
      for (ValueDecoderMiddleware<T> middleware : decoderMiddleware) {
        data = middleware.process(identifier, rawData, objectMapper, data, ignoreCache);
      }
    } catch (JacksonYAMLParseException e) {
      if (Objects.nonNull(e.getMessage())
          && e.getMessage().contains("incoming YAML document exceeds the limit")) {
        throw new IOException(
            String.format(
                "Maximum YAML file size of %s exceeded, increase 'store.maxYamlFileSize' to fix.",
                maxYamlFileSize),
            e);
      }
      data = tryRecovery(identifier, rawData, objectMapper, e);
    } catch (Throwable e) {
      data = tryRecovery(identifier, rawData, objectMapper, e);
    }

    return data;
  }

  private T tryRecovery(
      Identifier identifier, byte[] rawData, ObjectMapper objectMapper, Throwable originalException)
      throws IOException {
    Optional<ValueDecoderMiddleware<T>> recovery =
        decoderMiddleware.stream().filter(ValueDecoderMiddleware::canRecover).findFirst();

    if (recovery.isPresent()) {
      try {
        return recovery.get().recover(identifier, rawData, objectMapper);
      } catch (Throwable recoveryException) {
        originalException.addSuppressed(recoveryException);
      }
    }

    rethrowOriginalException(originalException);
    return null; // unreachable
  }

  private void rethrowOriginalException(Throwable originalException) throws IOException {
    if (originalException instanceof IOException) {
      throw (IOException) originalException;
    }
    if (originalException instanceof RuntimeException) {
      throw (RuntimeException) originalException;
    }
    if (originalException instanceof Error) {
      throw (Error) originalException;
    }
    throw new IOException("Deserialization failed", originalException);
  }
}
