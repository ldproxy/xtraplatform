package de.ii.xtraplatform.streams.domain;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.streams.app.SinkDefault;
import de.ii.xtraplatform.streams.app.SinkDefault.Type;
import de.ii.xtraplatform.streams.app.SourceDefault;
import de.ii.xtraplatform.streams.app.TransformerDefault;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Reactive {

  Runner runner(String name);

  Runner runner(String name, int capacity, int queueSize);

  interface Source<T> {

    <U> Source<U> via(Transformer<T, U> transformer);

    default <U, V extends Source<U>> V via(TransformerCustomSource<T, U, V> transformer) {
      return transformer.getCustomSource(via((Transformer<T, U>) transformer));
    }

    <V> BasicStream<T, V> to(Sink<T, V> sink);

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

    //TODO: remove
    @Deprecated
    static <T> Source<T> akka(akka.stream.javadsl.Source<T, ?> akkaSource) {
      return new SourceDefault<>(akkaSource);
    }
  }

  interface Transformer<T, U> {

    static <T, U> Transformer<T, U> map(Function<T, U> function) {
      return new TransformerDefault<>(function);
    }

    static <T> Transformer<T, T> peek(Consumer<T> consumer) {
      return new TransformerDefault<>(consumer);
    }
  }

  interface TransformerCustom<T, U> extends Transformer<T, U> {

    void init(Consumer<U> push);

    void onPush(T t);

    void onComplete();
  }

  interface TranformerCustomFuseableIn<T, U, V> extends TransformerCustom<T, U> {

    V fuseableSink();
  }

  interface TranformerCustomFuseableOut<T, U, V> extends TransformerCustom<T, U> {

    Class<V> getFusionInterface();

    void fuse(TranformerCustomFuseableIn<U, ?, V> tranformerCustomFuseableIn);

    default boolean canFuse(TranformerCustomFuseableIn<U, ?, ?> tranformerCustomFuseableIn) {
      return getFusionInterface()
          .isAssignableFrom(tranformerCustomFuseableIn.fuseableSink().getClass());
    }
  }

  interface TranformerCustomFuseable<T, V> extends TranformerCustomFuseableIn<T, T, V>,
      TranformerCustomFuseableOut<T, T, V> {

  }

  interface TransformerCustomSource<T, U, V extends Source<U>> extends TransformerCustom<T, U> {
    V getCustomSource(Source<U> source);
  }

  interface Sink<U, V> {

    static <T> Sink<T, Void> ignore() {
      return new SinkDefault<>(Type.IGNORE);
    }

    static <T> Sink<T, T> head() {
      return new SinkDefault<>(Type.HEAD);
    }

    static <T> Sink<T, Void> subscriber(Flow.Subscriber<T> subscriber) {
      return new SinkDefault<>(subscriber);
    }

    static <T> Sink<T, Void> foreach(Consumer<T> consumer) {
      return new SinkDefault<>(consumer);
    }

    static <T, W> Sink<T, W> reduce(W zero, BiFunction<W, T, W> reducer) {
      return new SinkDefault<>(zero, reducer);
    }

    static Sink<byte[], Void> outputStream(OutputStream outputStream) {
      return new SinkDefault<>(outputStream);
    }
  }

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

    //2x
    @Deprecated
    <T, U, V> CompletionStage<V> run(akka.stream.javadsl.Source<T, U> source,
        akka.stream.javadsl.Sink<T, CompletionStage<V>> sink);

    //5x
    @Deprecated
    <U> CompletionStage<U> run(RunnableGraphWrapper<U> graph);

    <X> CompletionStage<X> run(Stream<X> stream);

    int getCapacity();
  }

}
