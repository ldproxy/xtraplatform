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
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
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
  public InputStream postAsInputStream(String url, byte[] body, MediaType mediaType) {
    HttpPost httpPost = new HttpPost(url);
    httpPost.setEntity(new ByteArrayEntity(body, ContentType.parse(mediaType.toString())));

    if (Objects.nonNull(httpPost.getURI().getUserInfo())) {
      byte[] encodedAuth =
          Base64.encodeBase64(
              httpPost.getURI().getUserInfo().getBytes(StandardCharsets.ISO_8859_1));
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
      httpPost.setURI(URI.create(new URIBuilder(httpPost.getURI()).setUserInfo(null).toString()));
    }

    return getAsInputStream(httpClient, httpPost);
  }

  private static InputStream getAsInputStream(CloseableHttpClient client, HttpUriRequest request) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("HTTP {} request: {}", request.getMethod(), request.getURI());
    }

    try {
      CloseableHttpResponse response = client.execute(request);
      return response.getEntity().getContent();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
