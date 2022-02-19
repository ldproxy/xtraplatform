/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.util.Triple;
import de.ii.xtraplatform.streams.domain.Reactive;
import hu.akarnokd.rxjava3.operators.Flowables;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;

@Singleton
@AutoBind
public class ReactiveRx implements Reactive {

  @Inject
  public ReactiveRx() {}

  @Override
  public Runner runner(String name) {
    return new RunnerRx(name);
  }

  @Override
  public Runner runner(String name, int capacity, int queueSize) {
    return new RunnerRx(name, capacity, queueSize);
  }

  static <V, W> Triple<Flowable<V>, SubscriberRx<V>, StreamContext<W>> getGraph(Stream<W> stream) {
    if (stream instanceof StreamDefault) {
      return getGraph((StreamDefault<V, W>) stream);
    }

    if (stream instanceof StreamDefault.WithFinalizer) {
      return getGraph((StreamDefault<V, ?>.WithFinalizer<W>) stream);
    }

    throw new IllegalStateException();
  }

  // TODO: might use ConnectableFlowable
  static <V, W> Triple<Flowable<V>, SubscriberRx<V>, StreamContext<W>> getGraph(
      StreamDefault<V, W> stream) {
    Flowable<V> source = assemble(stream.getSource());
    SubscriberRx<V> sink = assemble(stream.getSink(), stream);
    AtomicReference<W> result = stream.getResult();

    if (stream.getItemHandler().isPresent()) {
      source =
          source.doOnNext(v -> result.getAndUpdate(w -> stream.getItemHandler().get().apply(w, v)));
    }

    return Triple.of(source, sink, stream);
  }

  static <V, W, X> Triple<Flowable<V>, SubscriberRx<V>, StreamContext<X>> getGraph(
      StreamDefault<V, W>.WithFinalizer<X> stream) {
    Triple<Flowable<V>, SubscriberRx<V>, StreamContext<W>> graph = getGraph(stream.getStream());
    Flowable<V> source = graph.first();
    SubscriberRx<V> sink = graph.second();

    return Triple.of(source, sink, stream);
  }

  static <U> io.reactivex.rxjava3.core.Flowable<U> assemble(Reactive.Source<U> source) {
    io.reactivex.rxjava3.core.Flowable<U> assembled = null;
    Optional<Source<U>> prepend = Optional.empty();
    Optional<Source<U>> mergeSorted = Optional.empty();
    Optional<Comparator<U>> mergeSortedComparator = Optional.empty();

    if (source instanceof SourceDefault) {
      assembled = assemble((SourceDefault<U>) source);
      prepend = ((SourceDefault<U>) source).getPrepend();
      mergeSorted = ((SourceDefault<U>) source).getMergeSorted();
      mergeSortedComparator = ((SourceDefault<U>) source).getMergeSortedComparator();
    }

    if (source instanceof SourceTransformed) {
      assembled = assemble((SourceTransformed<?, U>) source);
    }

    if (Objects.nonNull(assembled)) {
      if (prepend.isPresent()) {
        assembled = assembled.startWith(assemble(prepend.get()));
      }
      if (mergeSorted.isPresent() && mergeSortedComparator.isPresent()) {
        assembled =
            Flowables.orderedMerge(
                mergeSortedComparator.get(), assembled, assemble(mergeSorted.get()));
      }

      return assembled;
    }

    throw new IllegalStateException();
  }

  static <U> Flowable<U> assemble(SourceDefault<U> source) {
    switch (source.getType()) {
      case ITERABLE:
        return Flowable.fromIterable(source.getIterable());
      case PUBLISHER:
        return Flowable.fromPublisher(source.getPublisher());
      case SINGLE:
        return Flowable.just(source.getItem());
      case INPUT_STREAM:
        return (Flowable<U>)
            Flowable.fromPublisher(
                    FlowAdapters.toPublisher(BodyPublishers.ofInputStream(source::getInputStream)))
                .map(
                    byteBuffer ->
                        Arrays.copyOfRange(
                            byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()));
    }

    throw new IllegalStateException();
  }

  static <U, V> Flowable<V> assemble(SourceTransformed<U, V> source) {

    SourceDefault<U> source1 = source.getSource();
    Flowable<U> akkaSource = assemble(source1);
    Transformer<U, V> transformer = source.getTransformer();

    return assemble(akkaSource, transformer);
  }

  static <U, V> Flowable<V> assemble(Flowable<U> flowable, Transformer<U, V> transformer) {
    Flowable<V> assembled = null;
    Optional<Source<V>> prepend = Optional.empty();
    Optional<Source<V>> mergeSorted = Optional.empty();
    Optional<Comparator<V>> mergeSortedComparator = Optional.empty();

    if (transformer instanceof TransformerDefault) {
      assembled = assemble(flowable, (TransformerDefault<U, V>) transformer);
      prepend = ((TransformerDefault<U, V>) transformer).getPrepend();
      mergeSorted = ((TransformerDefault<U, V>) transformer).getMergeSorted();
      mergeSortedComparator = ((TransformerDefault<U, V>) transformer).getMergeSortedComparator();
    }

    if (transformer instanceof TransformerChained) {
      assembled = assemble(flowable, (TransformerChained<U, ?, V>) transformer);
    }

    if (transformer instanceof TransformerCustom) {
      assembled = new AsymmetricFlow<>(flowable, (TransformerCustom<U, V>) transformer).flow;
    }

    if (Objects.nonNull(assembled)) {
      if (prepend.isPresent()) {
        assembled = assembled.startWith(assemble(prepend.get()));
      }
      if (mergeSorted.isPresent() && mergeSortedComparator.isPresent()) {
        assembled =
            Flowables.orderedMerge(
                mergeSortedComparator.get(), assembled, assemble(mergeSorted.get()));
      }

      return assembled;
    }

    throw new IllegalStateException();
  }

  static <U, V> Flowable<V> assemble(Flowable<U> flowable, TransformerDefault<U, V> transformer) {
    switch (transformer.getType()) {
      case MAP:
        return flowable.map(transformer.getFunction()::apply);
      case FILTER:
        return (Flowable<V>) flowable.filter(u -> transformer.getPredicate().test(u));
      case PEEK:
        return flowable.map(
            u -> {
              transformer.getConsumer().accept(u);
              return (V) u;
            });
      case REDUCE:
        return flowable.reduce(transformer.getItem(), transformer.getReducer()::apply).toFlowable();
      case FLATMAP:
        return flowable.concatMap(u -> assemble(transformer.getFlatMap().apply(u)));
    }

    throw new IllegalStateException();
  }

  static <U, V, W> Flowable<W> assemble(
      Flowable<U> akkaSource, TransformerChained<U, V, W> transformer) {
    Transformer<U, V> transformer1 = transformer.getTransformer1();
    Transformer<V, W> transformer2 = transformer.getTransformer2();
    Flowable<V> akkaSource1 = assemble(akkaSource, transformer1);
    Flowable<W> akkaSource2 = assemble(akkaSource1, transformer2);

    return akkaSource2;
  }

  static <U, V> SubscriberRx<U> assemble(SinkReduced<U, V> sink, StreamContext<V> stream) {
    if (sink instanceof SinkDefault) {
      return assemble((SinkDefault<U, V>) sink, stream);
    }
    if (sink instanceof SinkTransformedImpl) {
      return assemble((SinkTransformedImpl<U, ?, V>) sink, stream);
    }

    throw new IllegalStateException();
  }

  static <U, V> SubscriberRx<U> assemble(SinkDefault<U, V> sink, StreamContext<V> stream) {
    switch (sink.getType()) {
      case IGNORE:
        return new SubscriberRx<U>() {
          @Override
          public void onNext(U u) {}
        };
      case HEAD:
        return new SubscriberRx<U>() {
          boolean first = true;

          @Override
          public void onNext(U u) {
            if (first) {
              first = false;
              stream.getResult().getAndUpdate(v -> (V) u);
            }
          }
        };
      case SUBSCRIBER:
        return new SubscriberRxWrapper<>(FlowAdapters.toSubscriber(sink.getSubscriber()));
      case FOREACH:
        return new SubscriberRx<>() {
          @Override
          public void onNext(U u) {
            try {
              sink.getConsumer().accept(u);
            } catch (Throwable throwable) {
              handleError(throwable);
            }
          }
        };
      case REDUCE:
        return new SubscriberRx<>() {
          @Override
          public void onNext(U u) {
            stream
                .getResult()
                .getAndUpdate(
                    v -> {
                      try {
                        return sink.getReducer().apply(v, u);
                      } catch (Throwable throwable) {
                        handleError(throwable);
                      }
                      return v;
                    });
          }
        };
      case OUTPUT_STREAM:
        return (SubscriberRx<U>)
            new SubscriberRx<byte[]>() {
              @Override
              public void onNext(byte[] bytes) {
                try {
                  sink.getOutputStream().write(bytes);
                } catch (Throwable throwable) {
                  handleError(throwable);
                }
              }
            };
    }

    throw new IllegalStateException();
  }

  // TODO: test
  static <U, V, W> SubscriberRx<U> assemble(
      SinkTransformedImpl<U, V, W> sink, StreamContext<W> stream) {
    UnicastProcessor<U> subscriber = UnicastProcessor.create();

    Flowable<V> transformed = assemble(subscriber, sink.getTransformer());

    SubscriberRx<V> assembled = assemble(sink.getSink(), stream);

    transformed.subscribe(assembled);

    return new SubscriberRxWrapper<>(subscriber);
  }

  abstract static class SubscriberRx<T> extends DefaultSubscriber<T> {
    private Consumer<Throwable> throwableConsumer;

    SubscriberRx<T> onError(Consumer<Throwable> consumer) {
      this.throwableConsumer = consumer;
      return this;
    }

    void handleError(Throwable throwable) {
      if (Objects.nonNull(throwableConsumer)) {
        throwableConsumer.accept(throwable);
      }
      cancel();
    }

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}
  }

  static class SubscriberRxWrapper<T> extends SubscriberRx<T> {

    private final Subscriber<T> delegate;

    private SubscriberRxWrapper(Subscriber<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onNext(T t) {
      delegate.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
      delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
      delegate.onComplete();
    }
  }

  // TODO: creating or clearing Iterables hurts performance
  // when switching to RxJava, the correspondent operator would be concatMapIterable
  // but the more performant alternative might be partialCollect from extensions
  private static class AsymmetricFlow<U, V> {

    final List<V> items;
    final Flowable<V> flow;

    AsymmetricFlow(Flowable<U> flowable, TransformerCustom<U, V> transformerCustom) {
      this.items = new ArrayList<>();
      this.flow =
          flowable
              .concatMapIterable(
                  u -> {
                    items.clear();

                    transformerCustom.onPush(u);

                    return items;
                  })
              // TODO: lazy
              .concatWith(
                  Flowable.fromIterable(
                      () -> {
                        items.clear();
                        transformerCustom.onComplete();
                        return items.listIterator();
                      }));

      transformerCustom.init(items::add);
    }
  }
}
