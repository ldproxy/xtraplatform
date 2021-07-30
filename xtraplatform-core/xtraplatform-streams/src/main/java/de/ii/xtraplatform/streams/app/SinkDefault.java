/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import java.io.OutputStream;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class SinkDefault<U, V> implements SinkReduced<U, V> {

  public enum Type {
    IGNORE,
    HEAD,
    SUBSCRIBER,
    FOREACH,
    REDUCE,
    OUTPUT_STREAM
  }

  private final Type type;
  private final Flow.Subscriber<U> subscriber;
  private final Consumer<U> consumer;
  private final V item;
  private final BiFunction<V, U, V> reducer;
  private final OutputStream outputStream;

  public SinkDefault(Type type) {
    this(type, null, null, null, null, null);
  }

  public SinkDefault(Flow.Subscriber<U> subscriber) {
    this(Type.SUBSCRIBER, subscriber, null, null, null, null);
  }

  public SinkDefault(Consumer<U> consumer) {
    this(Type.FOREACH, null, consumer, null, null, null);
  }

  public SinkDefault(V item, BiFunction<V, U, V> reducer) {
    this(Type.REDUCE, null, null, item, reducer, null);
  }

  public SinkDefault(OutputStream outputStream) {
    this(Type.OUTPUT_STREAM, null, null, null, null, outputStream);
  }

  SinkDefault(
      Type type,
      Flow.Subscriber<U> subscriber,
      Consumer<U> consumer,
      V item,
      BiFunction<V, U, V> reducer,
      OutputStream outputStream) {
    this.type = type;
    this.subscriber = subscriber;
    this.consumer = consumer;
    this.item = item;
    this.reducer = reducer;
    this.outputStream = outputStream;
  }

  <V1> SinkDefault<U, V1> withResult(V1 item) {
    return new SinkDefault<U, V1>(type, subscriber, consumer, item, null, outputStream);
  }

  public Type getType() {
    return type;
  }

  public Subscriber<U> getSubscriber() {
    return subscriber;
  }

  public Consumer<U> getConsumer() {
    return consumer;
  }

  public V getItem() {
    return item;
  }

  public BiFunction<V, U, V> getReducer() {
    return reducer;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }
}
