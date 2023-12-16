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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.MediaType;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
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
    HttpPost httpPost = new HttpPost(url);
    URI uri = URI.create(url);
    httpPost.addHeader("Content-Type", mediaType.toString());
    headers.forEach(httpPost::addHeader);
    httpPost.setEntity(new ByteArrayEntity(body, ContentType.parse(mediaType.toString())));

    if (Objects.nonNull(uri.getUserInfo())) {
      byte[] encodedAuth =
          Base64.encodeBase64(uri.getUserInfo().getBytes(StandardCharsets.ISO_8859_1));
      httpPost.addHeader("Authorization", "Basic " + new String(encodedAuth));
      httpPost.setUri(URI.create(new URIBuilder(uri).setUserInfo(null).toString()));
    }

    return getAsInputStream(httpClient, httpPost);
  }

  private static InputStream getAsInputStream(CloseableHttpClient client, HttpUriRequest request) {
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

    try {
      CloseableHttpResponse response = client.execute(request);
      return response.getEntity().getContent();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
