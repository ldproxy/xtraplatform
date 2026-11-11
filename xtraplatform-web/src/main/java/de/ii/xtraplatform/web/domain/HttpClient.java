/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

public interface HttpClient {
  Reactive.Source<byte[]> get(String url);

  Source<byte[]> getAsSource(InputStream inputStream);

  InputStream getAsInputStream(String url);

  InputStream getAsInputStream(String url, Map<String, String> headers);

  /**
   * Sends a POST request and returns the body of the response, whatever its status code. Use {@link
   * #post(String, byte[], MediaType, Map)} when the status code matters.
   */
  InputStream postAsInputStream(
      String url, byte[] body, MediaType mediaType, Map<String, String> headers);

  /**
   * Sends a POST request and returns the status code and the body of the response.
   *
   * @throws IllegalArgumentException if the request cannot be sent or the response cannot be read
   */
  HttpResponse post(String url, byte[] body, MediaType mediaType, Map<String, String> headers);

  /**
   * Sends a request and returns the status code, the headers and the body of the response.
   * Redirects are not followed: a redirect is returned like any other response, so that the caller
   * decides whether to follow its location.
   *
   * @param method the HTTP method, e.g. {@code GET}
   * @param headers the headers of the request; the media type of a body is the header {@code
   *     Content-Type}
   * @param body the body of the request, empty for a request without a body
   * @param maximumSize the maximum size of the body of the response in bytes
   * @throws IllegalArgumentException if the request cannot be sent, the response cannot be read or
   *     the body of the response is larger than {@code maximumSize}
   */
  HttpResponse send(
      String method,
      String url,
      Map<String, String> headers,
      Optional<byte[]> body,
      long maximumSize);

  @Value.Immutable
  interface HttpResponse {

    static HttpResponse of(int status, byte[] body) {
      return ImmutableHttpResponse.of(status, body);
    }

    @Value.Parameter
    int getStatus();

    @Value.Parameter
    byte[] getBody();

    /**
     * The headers of the response by name in lower case; several values of a header are joined with
     * a comma. Only {@link HttpClient#send} returns the headers.
     */
    @Value.Default
    default Map<String, String> getHeaders() {
      return Map.of();
    }

    default boolean isSuccessful() {
      return getStatus() >= 200 && getStatus() < 300;
    }
  }
}
