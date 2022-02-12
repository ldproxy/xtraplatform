/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.domain;

import de.ii.xtraplatform.streams.app.SinkDefault;
import de.ii.xtraplatform.streams.app.SinkDefault.Type;
import de.ii.xtraplatform.streams.app.SinkTransformedImpl;
import de.ii.xtraplatform.streams.app.SourceDefault;
import de.ii.xtraplatform.streams.app.TransformerChained;
import de.ii.xtraplatform.streams.app.TransformerDefault;
import de.ii.xtraplatform.streams.app.TransformerFused;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import scala.concurrent.ExecutionContextExecutor;

public interface Reactive {

  Runner runner(String name);

  Runner runner(String name, int capacity, int queueSize);

  interface Source<T> {

    <U> Source<U> via(Transformer<T, U> transformer);

    default <U, V extends Source<U>> V via(TransformerCustomSource<T, U, V> transformer) {
      return transformer.getCustomSource(via((Transformer<T, U>) transformer));
    }

    <V> BasicStream<T, V> to(SinkReduced<T, V> sink);

    default BasicStream<T, Void> to(Sink<T> sink) {
      return to((SinkReduced<T, Void>) sink);
    }

    <V, W> BasicStream<V, W> to(SinkReducedTransformed<T, V, W> sink);

    default <V> BasicStream<V, Void> to(SinkTransformed<T, V> sink) {
      return to((SinkReducedTransformed<T, V, Void>) sink);
    }

    Source<T> mapError(Function<Throwable, Throwable> errorMapper);

    Source<T> prepend(Source<T> other);

    Source<T> mergeSorted(Source<T> other, Comparator<T> comparator);

    static <T> Source<T> iterable(Iterable<T> iterable) {
      return new SourceDefault<>(iterable);
    }

    static <T> Source<T> publisher(Flow.Publisher<T> publisher) {
      return new SourceDefault<>(publisher);
    }

    static <T> Source<T> single(T item) {
      return new SourceDefault<>(item);
    }

    static Source<byte[]> inputStream(InputStream inputStream) {
      return new SourceDefault<>(inputStream);
    }

    // TODO: remove
    @Deprecated
    static <T> Source<T> akka(akka.stream.javadsl.Source<T, ?> akkaSource) {
      return new SourceDefault<>(akkaSource);
    }
  }

  interface Transformer<T, U> {

    default <V> Transformer<T, V> via(Transformer<U, V> transformer) {
      return new TransformerChained<>(this, transformer);
    }

    default <W> SinkReducedTransformed<T, U, W> to(SinkReduced<U, W> sink) {
      return new SinkTransformedImpl<>(this, sink);
    }

    default <W, X> SinkReducedTransformed<T, X, W> to(SinkReducedTransformed<U, X, W> sink) {
      if (sink instanceof SinkTransformedImpl) {
        SinkReducedTransformed<T, X, W> merge = merge((SinkTransformedImpl<U, X, W>) sink);

        return merge;
      }
      throw new UnsupportedOperationException();
    }

    default <V, W> SinkReducedTransformed<T, V, W> merge(SinkTransformedImpl<U, V, W> sink) {
      Transformer<U, V> transformer = sink.getTransformer();
      SinkReduced<V, W> sink1 = sink.getSink();

      Transformer<T, V> via = via(transformer);
      SinkReducedTransformed<T, V, W> to = via.to(sink1);

      return to;
    }

    default SinkTransformed<T, U> to(Sink<U> sink) {
      return to((SinkReduced<U, Void>) sink);
    }

    default Transformer<T, U> prepend(Source<U> other) {
      throw new UnsupportedOperationException();
    }

    default Transformer<T, U> mergeSorted(Source<U> other, Comparator<U> comparator) {
      throw new UnsupportedOperationException();
    }

    static <T, U> Transformer<T, U> map(Function<T, U> function) {
      return new TransformerDefault<>(function);
    }

    static <T> Transformer<T, T> filter(Predicate<T> predicate) {
      return new TransformerDefault<>(predicate);
    }

    static <T> Transformer<T, T> peek(Consumer<T> consumer) {
      return new TransformerDefault<>(consumer);
    }

    static <T, U> Transformer<T, U> reduce(U zero, BiFunction<U, T, U> reducer) {
      return new TransformerDefault<>(zero, reducer);
    }

    static <T, U> Transformer<T, U> flatMap(Function<T, Source<U>> flatMap) {
      return new TransformerDefault<>(flatMap, true);
    }
  }

  interface TransformerCustom<T, U> extends Transformer<T, U> {

    @Override
    default <V> Transformer<T, V> via(Transformer<U, V> transformer) {
      return new TransformerChained<>(this, transformer);
    }

    void init(Consumer<U> push);

    void onPush(T t);

    void onComplete();
  }

  interface TransformerCustomFuseableIn<T, U, V> extends TransformerCustom<T, U> {

    V fuseableSink();

    void afterInit(Runnable runnable);
  }

  interface TranformerCustomFuseableOut<T, U, V> extends TransformerCustom<T, U> {

    @Override
    default <W> Transformer<T, W> via(Transformer<U, W> transformer) {
      if (transformer instanceof TransformerCustomFuseableIn
          && canFuse((TransformerCustomFuseableIn<U, W, ?>) transformer)) {
        return new TransformerFused<>(this, (TransformerCustomFuseableIn<U, W, V>) transformer);
      }

      return TransformerCustom.super.via(transformer);
    }

    Class<? extends V> getFusionInterface();

    void fuse(TransformerCustomFuseableIn<U, ?, ? extends V> transformerCustomFuseableIn);

    default boolean canFuse(TransformerCustomFuseableIn<U, ?, ?> transformerCustomFuseableIn) {
      return getFusionInterface()
          .isAssignableFrom(transformerCustomFuseableIn.fuseableSink().getClass());
    }
  }

  interface TransformerCustomFuseable<T, V>
      extends TransformerCustomFuseableIn<T, T, V>, TranformerCustomFuseableOut<T, T, V> {}

  interface TransformerCustomSource<T, U, V extends Source<U>> extends TransformerCustom<T, U> {

    V getCustomSource(Source<U> source);
  }

  /*interface TransformerCustomSink<T, U, V extends SinkWrapper<T>, W extends SinkWrapperReduced<T, ?>> extends TransformerCustom<T, U> {

    //TODO: does not work since SinkReduced<U, X> does not match SinkWrapperReduced<T, ?>
    <X> W to(SinkReduced<U, X> sink);

    default V to(Sink<U> sink) {
      return getCustomSink(TransformerCustom.super.to(sink));
    }

    V getCustomSink(Sink<T> sink);
  }

  interface SinkWrapper<T> extends Sink<T> {

    Sink<T> getDelegate();
  }

  interface SinkWrapperReduced<T, V> extends SinkReduced<T, V> {

    SinkReduced<T, V> getDelegate();
  }*/

  interface Sink<U> {
    static <T> Sink<T> ignore() {
      return new SinkDefault<>(Type.IGNORE);
    }

    static <T> SinkReduced<T, T> head() {
      return new SinkDefault<>(Type.HEAD);
    }

    static <T> Sink<T> subscriber(Flow.Subscriber<T> subscriber) {
      return new SinkDefault<>(subscriber);
    }

    static <T> Sink<T> foreach(Consumer<T> consumer) {
      return new SinkDefault<>(consumer);
    }

    static <T, W> SinkReduced<T, W> reduce(W zero, BiFunction<W, T, W> reducer) {
      return new SinkDefault<>(zero, reducer);
    }

    static Sink<byte[]> outputStream(OutputStream outputStream) {
      return new SinkDefault<>(outputStream);
    }

    static SinkReducedTransformed<byte[], byte[], byte[]> reduceByteArray() {
      Transformer<byte[], ByteArrayOutputStream> reduce =
          Transformer.reduce(
              new ByteArrayOutputStream(),
              (outputStream, bytes) -> {
                outputStream.writeBytes(bytes);
                return outputStream;
              });
      Transformer<ByteArrayOutputStream, byte[]> map =
          Transformer.map(ByteArrayOutputStream::toByteArray);

      return reduce.via(map).to(Sink.head());
    }

    // TODO: remove
    @Deprecated
    static <T, U> SinkReduced<T, U> akka(akka.stream.javadsl.Sink<T, CompletionStage<U>> akkaSink) {
      return new SinkDefault<>(akkaSink);
    }
  }

  interface SinkReduced<U, V> extends Sink<U> {}

  interface SinkTransformed<T, U> extends Sink<T> {}

  interface SinkReducedTransformed<T, U, V> extends SinkTransformed<T, U>, SinkReduced<T, V> {}

  interface Stream<V> {

    RunnableStream<V> on(Runner runner);
  }

  interface BasicStream<U, V> extends Stream<V> {

    <W> StreamWithResult<U, W> withResult(W initial);
  }

  interface StreamWithResult<U, W> extends Stream<W> {

    StreamWithResult<U, W> handleError(BiFunction<W, Throwable, W> errorHandler);

    StreamWithResult<U, W> handleItem(BiFunction<W, U, W> itemHandler);

    <X> Stream<X> handleEnd(Function<W, X> finalizer);
  }

  interface RunnableStream<X> {

    CompletionStage<X> run();
  }

  interface Runner extends Closeable {

    int DYNAMIC_CAPACITY = -1;

    // 2x
    @Deprecated
    <T, U, V> CompletionStage<V> run(
        akka.stream.javadsl.Source<T, U> source,
        akka.stream.javadsl.Sink<T, CompletionStage<V>> sink);

    // 5x
    @Deprecated
    <U> CompletionStage<U> run(RunnableGraphWrapper<U> graph);

    <X> CompletionStage<X> run(Stream<X> stream);

    @Deprecated
    ExecutionContextExecutor getDispatcher();

    int getCapacity();
  }
}
