/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Creator;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JavaFlowSupport;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

@Component
@Provides
@Instantiate
public class ReactiveAkka implements Reactive {

  private final BundleContext context;
  private final ActorSystemProvider actorSystemProvider;

  public ReactiveAkka(
      @Context BundleContext context, @Requires ActorSystemProvider actorSystemProvider) {
    this.context = context;
    this.actorSystemProvider = actorSystemProvider;
  }

  @Override
  public Runner runner(String name) {
    return new RunnerAkka(context, actorSystemProvider, name);
  }

  @Override
  public Runner runner(String name, int capacity, int queueSize) {
    return new RunnerAkka(context, actorSystemProvider, name, capacity, queueSize);
  }

  static <W> RunnableGraph<CompletionStage<W>> getGraph(Stream<W> stream) {
    if (stream instanceof StreamDefault) {
      return getGraph((StreamDefault<?, W>) stream);
    }

    if (stream instanceof StreamDefault.WithFinalizer) {
      return getGraph((StreamDefault<?, ?>.WithFinalizer<W>) stream);
    }

    throw new IllegalStateException();
  }

  static <V, W> RunnableGraph<CompletionStage<W>> getGraph(StreamDefault<V, W> stream) {
    akka.stream.javadsl.Source<V, ?> source = assemble(stream.getSource());
    akka.stream.javadsl.Sink<V, CompletionStage<W>> sink = assemble(stream.getSink());

    if (Objects.nonNull(stream.getResult())) {
      sink =
          sink.mapMaterializedValue(
              completionStage -> completionStage.thenApply(w -> stream.getResult()));
    }

    if (stream.getItemHandler().isPresent()) {
      akka.stream.javadsl.Sink<V, CompletionStage<W>> combinerSink =
          akka.stream.javadsl.Sink.fold(
              stream.getResult(),
              (result, item) -> stream.getItemHandler().get().apply(result, item));

      return source
          .alsoToMat(combinerSink, Keep.right())
          .toMat(sink, (left, right) -> stream.onComplete(right.thenCompose(r -> left)));
    }

    return source.toMat(sink, (left, right) -> stream.onComplete(right));
  }

  static <V, W, X> RunnableGraph<CompletionStage<X>> getGraph(
      StreamDefault<V, W>.WithFinalizer<X> stream) {
    RunnableGraph<CompletionStage<W>> graph = getGraph(stream.getStream());

    return graph.mapMaterializedValue(
        completionStage -> completionStage.thenApply(stream.getFinalizer()));
  }

  static <U> akka.stream.javadsl.Source<U, ?> assemble(Reactive.Source<U> source) {
    akka.stream.javadsl.Source<U, ?> assembled = null;
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
        assembled = assembled.prepend(assemble(prepend.get()));
      }
      if (mergeSorted.isPresent() && mergeSortedComparator.isPresent()) {
        assembled = assembled.mergeSorted(assemble(mergeSorted.get()), mergeSortedComparator.get());
      }

      return assembled;
    }

    throw new IllegalStateException();
  }

  static <U> akka.stream.javadsl.Source<U, ?> assemble(SourceDefault<U> source) {
    switch (source.getType()) {
      case ITERABLE:
        return akka.stream.javadsl.Source.from(source.getIterable());
      case PUBLISHER:
        return JavaFlowSupport.Source.fromPublisher(source.getPublisher());
      case SINGLE:
        return akka.stream.javadsl.Source.single(source.getItem());
      case INPUT_STREAM:
        return (akka.stream.javadsl.Source<U, ?>)
            StreamConverters.fromInputStream(source::getInputStream).map(ByteString::toArray);
      case AKKA:
        return source.getAkkaSource();
    }

    throw new IllegalStateException();
  }

  static <U, V> akka.stream.javadsl.Source<V, ?> assemble(SourceTransformed<U, V> source) {

    SourceDefault<U> source1 = source.getSource();
    akka.stream.javadsl.Source<U, ?> akkaSource = assemble(source1);
    Transformer<U, V> transformer = source.getTransformer();

    return assemble(akkaSource, transformer);
  }

  static <U, V> akka.stream.javadsl.Source<V, ?> assemble(
      akka.stream.javadsl.Source<U, ?> akkaSource, Transformer<U, V> transformer) {
    if (transformer instanceof TransformerDefault) {
      Flow<U, V, ?> flow = assemble((TransformerDefault<U, V>) transformer);
      return akkaSource.via(flow);
    }

    if (transformer instanceof TransformerChained) {
      return assemble(akkaSource, (TransformerChained<U, ?, V>) transformer);
    }

    if (transformer instanceof TransformerCustom) {
      Flow<U, V, ?> flow = new AsymmetricFlow<>((TransformerCustom<U, V>) transformer).flow;
      return akkaSource.via(flow);
    }

    throw new IllegalStateException();
  }

  static <U, V> akka.stream.javadsl.Flow<U, V, ?> assemble(Transformer<U, V> transformer) {
    Flow<U, V, ?> assembled = null;
    Optional<Source<V>> prepend = Optional.empty();
    Optional<Source<V>> mergeSorted = Optional.empty();
    Optional<Comparator<V>> mergeSortedComparator = Optional.empty();

    if (transformer instanceof TransformerDefault) {
      assembled = assemble((TransformerDefault<U, V>) transformer);
      prepend = ((TransformerDefault<U, V>) transformer).getPrepend();
      mergeSorted = ((TransformerDefault<U, V>) transformer).getMergeSorted();
      mergeSortedComparator = ((TransformerDefault<U, V>) transformer).getMergeSortedComparator();
    }

    if (transformer instanceof TransformerChained) {
      assembled = assemble((TransformerChained<U, ?, V>) transformer);
    }

    if (transformer instanceof TransformerCustom) {
      assembled = new AsymmetricFlow<>((TransformerCustom<U, V>) transformer).flow;
    }

    if (Objects.nonNull(assembled)) {
      if (prepend.isPresent()) {
        assembled = assembled.prepend(assemble(prepend.get()));
      }
      if (mergeSorted.isPresent() && mergeSortedComparator.isPresent()) {
        assembled = assembled.mergeSorted(assemble(mergeSorted.get()), mergeSortedComparator.get());
      }

      return assembled;
    }

    throw new IllegalStateException();
  }

  static <U, V> akka.stream.javadsl.Flow<U, V, ?> assemble(TransformerDefault<U, V> transformer) {
    switch (transformer.getType()) {
      case MAP:
        return akka.stream.javadsl.Flow.fromFunction(transformer.getFunction()::apply);
      case FILTER:
        akka.stream.javadsl.Flow<U, U, NotUsed> flow1 = akka.stream.javadsl.Flow.create();
        return (Flow<U, V, ?>) flow1.filter(u -> transformer.getPredicate().test(u));
      case PEEK:
        return akka.stream.javadsl.Flow.fromFunction(
            u -> {
              transformer.getConsumer().accept(u);
              return (V) u;
            });
      case REDUCE:
        akka.stream.javadsl.Flow<U, U, NotUsed> flow2 = akka.stream.javadsl.Flow.create();
        return flow2.fold(transformer.getItem(), transformer.getReducer()::apply);
      case FLATMAP:
        akka.stream.javadsl.Flow<U, U, NotUsed> flow3 = akka.stream.javadsl.Flow.create();
        return flow3.flatMapConcat(
            u -> {
              Source<V> source = transformer.getFlatMap().apply(u);
              akka.stream.javadsl.Source<V, ?> akkaSource = assemble(source);
              return akkaSource;
            });
    }

    throw new IllegalStateException();
  }

  static <U, V, W> akka.stream.javadsl.Source<W, ?> assemble(
      akka.stream.javadsl.Source<U, ?> akkaSource, TransformerChained<U, V, W> transformer) {
    Transformer<U, V> transformer1 = transformer.getTransformer1();
    Transformer<V, W> transformer2 = transformer.getTransformer2();
    akka.stream.javadsl.Source<V, ?> akkaSource1 = assemble(akkaSource, transformer1);
    akka.stream.javadsl.Source<W, ?> akkaSource2 = assemble(akkaSource1, transformer2);

    return akkaSource2;
  }

  static <U, V, W> akka.stream.javadsl.Flow<U, W, ?> assemble(
      TransformerChained<U, V, W> transformer) {
    Transformer<U, V> transformer1 = transformer.getTransformer1();
    Transformer<V, W> transformer2 = transformer.getTransformer2();
    akka.stream.javadsl.Flow<U, V, ?> akkaFlow1 = assemble(transformer1);
    akka.stream.javadsl.Flow<V, W, ?> akkaFlow2 = assemble(transformer2);

    return akkaFlow1.via(akkaFlow2);
  }

  static <U, V> akka.stream.javadsl.Sink<U, CompletionStage<V>> assemble(SinkReduced<U, V> sink) {
    if (sink instanceof SinkDefault) {
      return assemble((SinkDefault<U, V>) sink);
    }
    if (sink instanceof SinkTransformedImpl) {
      return assemble((SinkTransformedImpl<U, ?, V>) sink);
    }

    throw new IllegalStateException();
  }

  static <U, V, W> akka.stream.javadsl.Sink<U, CompletionStage<W>> assemble(
      SinkTransformedImpl<U, V, W> sink) {

    SinkReduced<V, W> sink1 = sink.getSink();
    akka.stream.javadsl.Sink<V, CompletionStage<W>> akkaSink = assemble(sink1);
    Transformer<U, V> transformer = sink.getTransformer();

    return assemble(transformer, akkaSink);
  }

  static <U, V, W> akka.stream.javadsl.Sink<U, CompletionStage<W>> assemble(
      Transformer<U, V> transformer, akka.stream.javadsl.Sink<V, CompletionStage<W>> akkaSink) {
    if (transformer instanceof TransformerDefault) {
      Flow<U, V, ?> flow = assemble((TransformerDefault<U, V>) transformer);
      return flow.toMat(akkaSink, Keep.right());
    }

    if (transformer instanceof TransformerChained) {
      Flow<U, V, ?> flow = assemble((TransformerChained<U, ?, V>) transformer);
      return flow.toMat(akkaSink, Keep.right());
    }

    if (transformer instanceof TransformerCustom) {
      Flow<U, V, ?> flow = new AsymmetricFlow<>((TransformerCustom<U, V>) transformer).flow;
      return flow.toMat(akkaSink, Keep.right());
    }

    throw new IllegalStateException();
  }

  static <U, V> akka.stream.javadsl.Sink<U, CompletionStage<V>> assemble(SinkDefault<U, V> sink) {
    switch (sink.getType()) {
      case IGNORE:
        akka.stream.javadsl.Sink<U, CompletionStage<Done>> sink1 =
            akka.stream.javadsl.Sink.ignore();
        akka.stream.javadsl.Sink<U, CompletionStage<V>> sink2 =
            sink1.mapMaterializedValue(completionStage -> completionStage.thenApply(done -> null));
        return sink2;
      case HEAD:
        akka.stream.javadsl.Sink<U, CompletionStage<V>> sink3 =
            (akka.stream.javadsl.Sink<U, CompletionStage<V>>)
                akka.stream.javadsl.Sink.head()
                    .mapMaterializedValue(completionStage -> completionStage.thenApply(u -> (V) u));
        return sink3;
      case SUBSCRIBER:
        // TODO: how to get CompletionStage
        akka.stream.javadsl.Sink<U, NotUsed> sink4 =
            JavaFlowSupport.Sink.fromSubscriber(sink.getSubscriber());
        return null;
      case FOREACH:
        akka.stream.javadsl.Sink<U, CompletionStage<Done>> sink5 =
            akka.stream.javadsl.Sink.foreach(sink.getConsumer()::accept);
        akka.stream.javadsl.Sink<U, CompletionStage<V>> sink6 =
            sink5.mapMaterializedValue(completionStage -> completionStage.thenApply(done -> null));
        return sink6;
      case REDUCE:
        akka.stream.javadsl.Sink<U, CompletionStage<V>> sink7 =
            akka.stream.javadsl.Sink.fold(sink.getItem(), sink.getReducer()::apply);
        return sink7;
      case OUTPUT_STREAM:
        Flow<byte[], byte[], NotUsed> flow = Flow.create();
        // TODO: creates a copy
        Flow<byte[], ByteString, NotUsed> flow2 = flow.map(ByteString::fromArray);
        akka.stream.javadsl.Sink<ByteString, CompletionStage<V>> sink8 =
            StreamConverters.fromOutputStream(sink::getOutputStream)
                .mapMaterializedValue(completionStage -> completionStage.thenApply(done -> null));
        akka.stream.javadsl.Sink<byte[], CompletionStage<V>> sink9 =
            flow2.toMat(sink8, Keep.right());
        return (akka.stream.javadsl.Sink<U, CompletionStage<V>>) sink9;
      case AKKA:
        return sink.getAkkaSink();
    }

    throw new IllegalStateException();
  }

  // TODO: creating or clearing Iterables hurts performance
  // when switching to RxJava, the correspondent operator would be concatMapIterable
  // but the more performant alternative might be partialCollect from extensions
  private static class AsymmetricFlow<U, V> {

    final List<V> items;
    final Flow<U, V, NotUsed> flow;

    AsymmetricFlow(TransformerCustom<U, V> transformerCustom) {
      this.items = new ArrayList<>();

      this.flow =
          Flow.<U>create()
              .mapConcat(
                  u -> {
                    items.clear();

                    transformerCustom.onPush(u);

                    return items;
                  })
              .concatLazy(
                  akka.stream.javadsl.Source.fromIterator(
                      (Creator<Iterator<V>>)
                          () -> {
                            items.clear();
                            transformerCustom.onComplete();
                            return items.listIterator();
                          }));

      transformerCustom.init(items::add);
    }
  }
}
