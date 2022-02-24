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
import javax.ws.rs.core.MediaType;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpClientApache implements HttpClient {

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
  public InputStream postAsInputStream(String url, byte[] body, MediaType mediaType) {
    HttpPost httpPost = new HttpPost(url);
    httpPost.setEntity(new ByteArrayEntity(body, ContentType.parse(mediaType.toString())));

    return getAsInputStream(httpClient, httpPost);
  }

  private static InputStream getAsInputStream(CloseableHttpClient client, HttpUriRequest request) {
    try {
      return client.execute(request).getEntity().getContent();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
