package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.io.InputStream;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

public class SourceDefault<T> implements Source<T> {

  enum Type {ITERABLE, PUBLISHER, SINGLE, INPUT_STREAM, AKKA}

  private final Type type;
  private final Iterable<T> iterable;
  private final Flow.Publisher<T> publisher;
  private final T item;
  private final InputStream inputStream;
  private final akka.stream.javadsl.Source<T, ?> akkaSource;

  public SourceDefault(Iterable<T> iterable) {
    this(Type.ITERABLE, iterable, null, null, null, null);
  }

  public SourceDefault(Flow.Publisher<T> publisher) {
    this(Type.PUBLISHER, null, publisher, null, null, null);
  }

  public SourceDefault(T item) {
    this(Type.SINGLE, null, null, item, null, null);
  }

  public SourceDefault(InputStream inputStream) {
    this(Type.INPUT_STREAM, null, null, null, inputStream, null);
  }

  //TODO: remove
  @Deprecated
  public SourceDefault(akka.stream.javadsl.Source<T, ?> akkaSource) {
    this(Type.AKKA, null, null, null, null, akkaSource);
  }

  SourceDefault(Type type, Iterable<T> iterable, Flow.Publisher<T> publisher, T item,
      InputStream inputStream, akka.stream.javadsl.Source<T, ?> akkaSource) {
    this.type = type;
    this.iterable = iterable;
    this.publisher = publisher;
    this.item = item;
    this.inputStream = inputStream;
    this.akkaSource = akkaSource;
  }


  @Override
  public <U> Source<U> via(Transformer<T, U> transformer) {
    return new SourceTransformed<>(this, transformer);
  }

  @Override
  public <V> BasicStream<T, V> to(Sink<T, V> sink) {
    return new StreamDefault<>(this, sink);
  }

  public Type getType() {
    return type;
  }

  public Iterable<T> getIterable() {
    return iterable;
  }

  public Publisher<T> getPublisher() {
    return publisher;
  }

  public T getItem() {
    return item;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public akka.stream.javadsl.Source<T, ?> getAkkaSource() {
    return akkaSource;
  }
}
