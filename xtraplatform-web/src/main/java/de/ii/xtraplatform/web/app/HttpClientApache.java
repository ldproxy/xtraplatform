/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.util.LambdaWithException;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import de.ii.xtraplatform.web.domain.HttpClient;
import de.ii.xtraplatform.web.domain.ImmutableHttpResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientApache implements HttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientApache.class);

  private final CloseableHttpClient httpClient;

  public HttpClientApache(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public Source<byte[]> get(String url) {
    return Source.iterable(ImmutableList.of(httpClient))
        .via(
            Transformer.flatMap(
                LambdaWithException.mayThrow(
                    client -> Source.inputStream(getAsInputStream(client, new HttpGet(url))))));
  }

  @Override
  public Source<byte[]> getAsSource(InputStream inputStream) {
    return Source.iterable(ImmutableList.of(httpClient))
        .via(
            Transformer.flatMap(
                LambdaWithException.mayThrow(client -> Source.inputStream(inputStream))));
  }

  @Override
  public InputStream getAsInputStream(String url) {
    return getAsInputStream(httpClient, new HttpGet(url));
  }

  @Override
  public InputStream getAsInputStream(String url, Map<String, String> headers) {
    HttpGet httpGet = new HttpGet(url);
    headers.forEach(httpGet::addHeader);

    return getAsInputStream(httpClient, httpGet);
  }

  @Override
  public InputStream postAsInputStream(
      String url, byte[] body, MediaType mediaType, Map<String, String> headers) {
    return getAsInputStream(httpClient, httpPost(url, body, mediaType, headers));
  }

  @Override
  public HttpResponse post(
      String url, byte[] body, MediaType mediaType, Map<String, String> headers) {
    HttpPost httpPost = httpPost(url, body, mediaType, headers);
    logRequest(httpPost);

    try {
      return httpClient.execute(
          httpPost,
          response ->
              HttpResponse.of(
                  response.getCode(),
                  Objects.nonNull(response.getEntity())
                      ? EntityUtils.toByteArray(response.getEntity())
                      : new byte[0]));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static HttpPost httpPost(
      String url, byte[] body, MediaType mediaType, Map<String, String> headers) {
    HttpPost httpPost = new HttpPost(url);
    URI uri = URI.create(url);
    httpPost.addHeader("Content-Type", mediaType.toString());
    headers.forEach(httpPost::addHeader);
    httpPost.setEntity(new ByteArrayEntity(body, ContentType.parse(mediaType.toString())));
    useUserInfo(httpPost, uri);

    return httpPost;
  }

  // the user info of the URI becomes basic authentication
  private static void useUserInfo(HttpUriRequestBase request, URI uri) {
    if (Objects.nonNull(uri.getUserInfo())) {
      byte[] encodedAuth =
          Base64.encodeBase64(uri.getUserInfo().getBytes(StandardCharsets.ISO_8859_1));
      request.addHeader(
          "Authorization", "Basic " + new String(encodedAuth, StandardCharsets.UTF_8));
      request.setUri(URI.create(new URIBuilder(uri).setUserInfo(null).toString()));
    }
  }

  @Override
  public HttpResponse send(
      String method,
      String url,
      Map<String, String> headers,
      Optional<byte[]> body,
      long maximumSize) {
    URI uri = URI.create(url);
    HttpUriRequestBase request = new HttpUriRequestBase(method, uri);
    headers.forEach(request::addHeader);
    body.ifPresent(
        content -> request.setEntity(new ByteArrayEntity(content, contentType(headers))));
    useUserInfo(request, uri);
    request.setConfig(withoutRedirects(httpClient));
    logRequest(request);

    try {
      return httpClient.execute(
          request,
          response ->
              ImmutableHttpResponse.of(
                      response.getCode(), content(response.getEntity(), maximumSize))
                  .withHeaders(headers(response)));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static ContentType contentType(Map<String, String> headers) {
    return headers.entrySet().stream()
        .filter(header -> "content-type".equalsIgnoreCase(header.getKey()))
        .map(header -> ContentType.parse(header.getValue()))
        .findFirst()
        .orElse(ContentType.APPLICATION_OCTET_STREAM);
  }

  // the settings of the client, e.g. the timeouts, apply to the request; only redirects are not
  // followed
  private static RequestConfig withoutRedirects(CloseableHttpClient client) {
    RequestConfig defaults =
        client instanceof Configurable configurable && Objects.nonNull(configurable.getConfig())
            ? configurable.getConfig()
            : RequestConfig.DEFAULT;
    return RequestConfig.copy(defaults).setRedirectsEnabled(false).build();
  }

  private static byte[] content(HttpEntity entity, long maximumSize) throws IOException {
    if (Objects.isNull(entity)) {
      return new byte[0];
    }
    if (entity.getContentLength() > maximumSize) {
      throw tooLarge(maximumSize);
    }
    try (InputStream content = entity.getContent()) {
      if (Objects.isNull(content)) {
        return new byte[0];
      }
      byte[] bytes = content.readNBytes((int) Math.min(maximumSize + 1, Integer.MAX_VALUE - 8));
      if (bytes.length > maximumSize) {
        throw tooLarge(maximumSize);
      }
      return bytes;
    }
  }

  private static IllegalArgumentException tooLarge(long maximumSize) {
    return new IllegalArgumentException(
        String.format("The body of the response is larger than %d bytes.", maximumSize));
  }

  private static Map<String, String> headers(ClassicHttpResponse response) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (Header header : response.getHeaders()) {
      headers.merge(
          header.getName().toLowerCase(Locale.ROOT),
          header.getValue(),
          (first, second) -> first + ", " + second);
    }
    return headers;
  }

  private static InputStream getAsInputStream(CloseableHttpClient client, HttpUriRequest request) {
    logRequest(request);

    try (CloseableHttpResponse response = client.execute(request)) {
      byte[] content = EntityUtils.toByteArray(response.getEntity());
      return new java.io.ByteArrayInputStream(content);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private static void logRequest(HttpUriRequest request) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("HTTP {} request: {}", request.getMethod(), request.getRequestUri());
      LOGGER.debug("HTTP Headers: {}", (Object) request.getHeaders());

      if (request instanceof HttpPost) {
        try {
          byte[] bytes = EntityUtils.toByteArray(((HttpPost) request).getEntity());
          LOGGER.debug("HTTP Body:\n{}", new String(bytes, StandardCharsets.UTF_8));
        } catch (Throwable e) {
          // ignore
        }
      }
    }
  }
}
