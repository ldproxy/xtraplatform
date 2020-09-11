/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.domain;

import akka.Done;
import akka.NotUsed;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpCharsets;
import akka.http.javadsl.model.MediaTypes;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.io.InputStream;
import java.util.concurrent.CompletionStage;

public interface HttpClient {

  // TODO: use Flow.Subscriber instead of akka Sink when upgrading to Java 11
  CompletionStage<Done> get(String url, Sink<ByteString, CompletionStage<Done>> sink);

  Source<ByteString, NotUsed> get(String url);

  CompletionStage<Done> post(
      String url,
      byte[] body,
      ContentType.NonBinary contentType,
      Sink<ByteString, CompletionStage<Done>> sink);

  String getAsString(String url);

  InputStream getAsInputStream(String url);

  InputStream postAsInputStream(String url, byte[] body, ContentType.NonBinary contentType);

  default CompletionStage<Done> post(
      String url,
      String body,
      ContentType.NonBinary contentType,
      Sink<ByteString, CompletionStage<Done>> sink) {
    return post(url, body.getBytes(), contentType, sink);
  }

  default CompletionStage<Done> postXml(
      String url, String xmlBody, Sink<ByteString, CompletionStage<Done>> sink) {
    return post(
        url,
        xmlBody.getBytes(),
        MediaTypes.APPLICATION_XML.toContentType(HttpCharsets.UTF_8),
        sink);
  }

  default InputStream postAsInputStream(
      String url, String body, ContentType.NonBinary contentType) {
    return postAsInputStream(url, body.getBytes(), contentType);
  }
}
