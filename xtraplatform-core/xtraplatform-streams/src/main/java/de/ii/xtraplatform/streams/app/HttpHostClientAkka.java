/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import akka.Done;
import akka.NotUsed;
import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.AcceptEncoding;
import akka.http.scaladsl.model.headers.HttpEncodings;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.japi.function.Procedure;
import akka.japi.pf.PFBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.QueueOfferResult;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.streams.domain.HttpClient;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

public class HttpHostClientAkka implements HttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHostClientAkka.class);

  private static final int MAX_QUEUED_REQUESTS = 128;

  private final SourceQueueWithComplete<Pair<HttpRequest, Object>> requestQueue;

  HttpHostClientAkka(
      ActorMaterializer materializer,
      Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool>
          connectionPool) {
    this.requestQueue = createRequestQueueForHostPool(materializer, connectionPool);
  }

  HttpHostClientAkka(
      ActorMaterializer materializer,
      Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> connectionPool,
      boolean superPool) {
    this.requestQueue = createRequestQueueForSuperPool(materializer, connectionPool);
  }

  @Override
  public CompletionStage<Done> get(String url, Sink<ByteString, CompletionStage<Done>> sink) {

    CompletionStage<CompletionStage<Done>> result = request(createHttpGet(url), sink);

    return result.thenCompose(doneCompletionStage -> doneCompletionStage);
  }

  // TODO
  @Override
  public Source<ByteString, NotUsed> get(String url) {
    return requestSource(createHttpGet(url));
  }

  @Override
  public CompletionStage<Done> post(
      String url,
      byte[] body,
      ContentType.NonBinary contentType,
      Sink<ByteString, CompletionStage<Done>> sink) {

    CompletionStage<CompletionStage<Done>> result =
        request(createHttpPost(url, body, contentType), sink);

    return result.thenCompose(doneCompletionStage -> doneCompletionStage);
  }

  @Override
  public String getAsString(String url) {
    Sink<ByteString, CompletionStage<StringBuilder>> asStringBuilder =
        Sink.fold(
            new StringBuilder(),
            (Function2<StringBuilder, ByteString, StringBuilder>)
                (stringBuilder, byteString) -> stringBuilder.append(byteString.utf8String()));

    StringBuilder response =
        request(createHttpGet(url), asStringBuilder)
            .toCompletableFuture()
            .join()
            .toCompletableFuture()
            .join();

    return response.toString();
  }

  @Override
  public InputStream getAsInputStream(String url) {

    return request(createHttpGet(url), StreamConverters.asInputStream())
        .toCompletableFuture()
        .join();
  }

  @Override
  public InputStream postAsInputStream(String url, byte[] body, ContentType.NonBinary contentType) {

    return request(createHttpPost(url, body, contentType), StreamConverters.asInputStream())
        .toCompletableFuture()
        .join();
  }

  // TODO: next offer is only allowed if previous returned, synchronize (only for
  // OverflowStrategy.backpressure)
  private <T> CompletionStage<T> request(HttpRequest httpRequest, Sink<ByteString, T> sink) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("HTTP {} request: {}", httpRequest.method().name(), httpRequest.getUri());
    }

    CompletableFuture<T> result = new CompletableFuture<>();
    CompletionStage<QueueOfferResult> offer =
        requestQueue.offer(Pair.create(httpRequest, Pair.create(sink, result)));

    offer.whenComplete(
        (queueOfferResult, throwable) -> {
          if (!Objects.equals(queueOfferResult, QueueOfferResult.enqueued())) {
            if (Objects.equals(queueOfferResult, QueueOfferResult.dropped())) {
              LOGGER.warn(
                  "Request dropped because queue for target host already has {} requests: {}",
                  MAX_QUEUED_REQUESTS,
                  httpRequest);
            } else {
              LOGGER.error("Request queueing failed");
            }

            Throwable throwable1;
            if (queueOfferResult instanceof QueueOfferResult.Failure) {
              throwable1 = ((QueueOfferResult.Failure) queueOfferResult).cause();
            } else if (Objects.nonNull(throwable)) {
              throwable1 = throwable;
            } else {
              throwable1 = new IllegalStateException();
            }
            result.completeExceptionally(throwable1);

            LogContext.error(LOGGER, throwable1, "Unexpected queuing exception");
          }
        });

    return result;
  }

  private Source<ByteString, NotUsed> requestSource(HttpRequest httpRequest) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("HTTP {} request: {}", httpRequest.method().name(), httpRequest.getUri());
    }

    CompletableFuture<Source<ByteString, NotUsed>> result = new CompletableFuture<>();
    CompletionStage<QueueOfferResult> offer =
        requestQueue.offer(Pair.create(httpRequest, Pair.create(null, result)));

    offer.whenComplete(
        (queueOfferResult, throwable) -> {
          if (!Objects.equals(queueOfferResult, QueueOfferResult.enqueued())) {
            if (Objects.equals(queueOfferResult, QueueOfferResult.dropped())) {
              LOGGER.warn(
                  "Request dropped because queue for target host already has {} requests: {}",
                  MAX_QUEUED_REQUESTS,
                  httpRequest);
            } else {
              LOGGER.error("Request queueing failed");
            }

            Throwable throwable1;
            if (queueOfferResult instanceof QueueOfferResult.Failure) {
              throwable1 = ((QueueOfferResult.Failure) queueOfferResult).cause();
            } else if (Objects.nonNull(throwable)) {
              throwable1 = throwable;
            } else {
              throwable1 = new IllegalStateException();
            }
            result.completeExceptionally(throwable1);

            LogContext.error(LOGGER, throwable1, "Unexpected queuing exception");
          }
        });

    return result.join();
  }

  private static SourceQueueWithComplete<Pair<HttpRequest, Object>> createRequestQueueForHostPool(
      ActorMaterializer materializer,
      Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool>
          connectionPool) {

    return createSourceQueue()
        .viaMat(connectionPool, Keep.left())
        .recover(
            new PFBuilder<Throwable, Pair<Try<HttpResponse>, Object>>()
                .matchAny(
                    throwable -> {
                      LogContext.error(LOGGER, throwable, "Unexpected queuing exception");
                      return null;
                    })
                .build())
        .toMat(Sink.foreach(handleResponse(materializer)), Keep.left())
        .run(materializer);
  }

  private static SourceQueueWithComplete<Pair<HttpRequest, Object>> createRequestQueueForSuperPool(
      ActorMaterializer materializer,
      Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> connectionPool) {

    return createSourceQueue()
        .viaMat(connectionPool, Keep.left())
        .recover(
            new PFBuilder<Throwable, Pair<Try<HttpResponse>, Object>>()
                .matchAny(
                    throwable -> {
                      LogContext.error(LOGGER, throwable, "Unexpected queuing exception");
                      return null;
                    })
                .build())
        .toMat(Sink.foreach(handleResponse(materializer)), Keep.left())
        .run(materializer);
  }

  private static Source<
          Pair<HttpRequest, Object>, SourceQueueWithComplete<Pair<HttpRequest, Object>>>
      createSourceQueue() {
    return Source.queue(MAX_QUEUED_REQUESTS, OverflowStrategy.dropHead());
  }

  // TODO: maybe create a container for httpResponse + sink + result to simplify stream
  // processing???
  private static <T> Procedure<Pair<Try<HttpResponse>, Object>> handleResponse(
      ActorMaterializer materializer) {
    return httpResponseAndSinkAndResult -> {
      Try<HttpResponse> httpResponse = httpResponseAndSinkAndResult.first();
      Pair<Sink<ByteString, T>, CompletableFuture<T>> sinkAndResult =
          (Pair<Sink<ByteString, T>, CompletableFuture<T>>) httpResponseAndSinkAndResult.second();
      Sink<ByteString, T> sink = sinkAndResult.first();
      CompletableFuture<T> result = sinkAndResult.second();

      handleResponse(httpResponse, sink, result, materializer);
    };
  }

  private static <T> void handleResponse(
      Try<HttpResponse> httpResponse,
      Sink<ByteString, T> sink,
      CompletableFuture<T> result,
      ActorMaterializer materializer) {

    if (httpResponse.isFailure()) {
      result.completeExceptionally(
          httpResponse
              .failed()
              .getOrElse(() -> new IllegalStateException("Unknown HTTP client error")));
      // TODO: do something with sink???
      return;
    }

    // return source
    if (Objects.isNull(sink)) {
      Source<ByteString, NotUsed> byteStringNotUsedSource =
          Source.single(httpResponse.get())
              .map(HttpHostClientAkka::decodeResponse)
              .flatMapConcat(
                  httpResponseDecoded -> {
                    if (LOGGER.isTraceEnabled())
                      LOGGER.trace("HTTP RESPONSE {}", httpResponseDecoded.status());
                    return httpResponseDecoded.entity().withoutSizeLimit().getDataBytes();
                  });
      result.complete((T) byteStringNotUsedSource);
      return;
    }

    // TODO: can't we handle this in the same stream???
    T t =
        Source.single(httpResponse.get())
            .map(HttpHostClientAkka::decodeResponse)
            .flatMapConcat(
                httpResponseDecoded -> {
                  if (LOGGER.isTraceEnabled())
                    LOGGER.trace("HTTP RESPONSE {}", httpResponseDecoded.status());
                  return httpResponseDecoded.entity().withoutSizeLimit().getDataBytes();
                })
            .runWith(sink, materializer);

    result.complete(t);
  }

  private static HttpRequest createHttpGet(String url) {
    return HttpRequest.GET(url).addHeader(ACCEPT);
  }

  private static HttpRequest createHttpPost(
      String url, byte[] body, ContentType.NonBinary contentType) {
    return HttpRequest.POST(url).withEntity(contentType, body).addHeader(ACCEPT);
  }

  private static final AcceptEncoding ACCEPT =
      AcceptEncoding.create(
          HttpEncodings.deflate().toRange(),
          HttpEncodings.gzip().toRange(),
          HttpEncodings.chunked().toRange());

  private static HttpResponse decodeResponse(HttpResponse response) {
    // Pick the right coder
    final Coder coder;
    if (Objects.equals(HttpEncodings.gzip(), response.encoding())) {
      coder = Coder.Gzip;
    } else if (Objects.equals(HttpEncodings.deflate(), response.encoding())) {
      coder = Coder.Deflate;
    } else {
      coder = Coder.NoCoding;
    }
    if (LOGGER.isTraceEnabled()) LOGGER.trace("HTTP Encoding {}", coder);

    // Decode the entity
    return coder.decodeMessage(response);
  }
}
