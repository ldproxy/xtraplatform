/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.RunnableStream;
import de.ii.xtraplatform.streams.domain.Reactive.Runner;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.Reactive.StreamContext;
import de.ii.xtraplatform.streams.domain.Reactive.StreamWithResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StreamDefault<V, W>
    implements BasicStream<V, W>, StreamWithResult<V, W>, StreamContext<W> {

  private final Source<V> source;
  private final SinkReduced<V, W> sink;
  private final AtomicReference<W> result;
  private final List<Function<Throwable, Throwable>> errorMappers;
  private Optional<BiFunction<W, Throwable, W>> errorHandler;
  private Optional<BiFunction<W, V, W>> itemHandler;

  public StreamDefault(Source<V> source, SinkReduced<V, W> sink) {
    this(
        source,
        sink,
        sink instanceof SinkDefault
            ? ((SinkDefault<V, W>) sink).getItem().get()
            : sink instanceof SinkTransformedImpl
                ? ((SinkTransformedImpl<?, ?, W>) sink).getItem().get()
                : null);
  }

  StreamDefault(Source<V> source, SinkReduced<V, W> sink, W initialResult) {
    this.source = source;
    this.sink = sink;
    this.result = new AtomicReference<>(initialResult);
    this.errorMappers = new ArrayList<>();
    this.errorHandler = Optional.empty();
    this.itemHandler = Optional.empty();

    if (source instanceof SourceDefault) {
      ((SourceDefault<V>) source).getErrorMapper().ifPresent(errorMappers::add);
    } else if (source instanceof SourceTransformed) {
      ((SourceTransformed<?, ?>) source).getSource().getErrorMapper().ifPresent(errorMappers::add);
    }
  }

  @Override
  public RunnableStream<W> on(Runner runner) {
    return new RunnableStreamDefault<>(runner, this);
  }

  @Override
  public <W1> StreamWithResult<V, W1> withResult(W1 initial) {
    if (sink instanceof SinkTransformedImpl) {
      boolean br = true;
    }
    return new StreamDefault<>(source, ((SinkDefault<V, W>) sink).withResult(initial), initial);
  }

  @Override
  public StreamWithResult<V, W> handleError(BiFunction<W, Throwable, W> errorHandler) {
    this.errorHandler = Optional.of(errorHandler);

    return this;
  }

  @Override
  public StreamWithResult<V, W> handleItem(BiFunction<W, V, W> itemHandler) {
    this.itemHandler = Optional.of(itemHandler);

    return this;
  }

  @Override
  public <X> Stream<X> handleEnd(Function<W, X> finalizer) {
    return new WithFinalizer<>(finalizer);
  }

  public Source<V> getSource() {
    return source;
  }

  public SinkReduced<V, W> getSink() {
    return sink;
  }

  @Override
  public AtomicReference<W> getResult() {
    return result;
  }

  public Optional<BiFunction<W, Throwable, W>> getErrorHandler() {
    return errorHandler;
  }

  public Optional<BiFunction<W, V, W>> getItemHandler() {
    return itemHandler;
  }

  @Override
  public void onComplete(CompletableFuture<W> resultFuture) {
    resultFuture.complete(result.get());
  }

  @Override
  public void onError(CompletableFuture<W> resultFuture, Throwable throwable) {
    Throwable actualThrowable =
        throwable instanceof CompletionException && Objects.nonNull(throwable.getCause())
            ? throwable.getCause()
            : throwable;
    if (errorHandler.isPresent()) {
      try {
        resultFuture.complete(errorHandler.get().apply(result.get(), actualThrowable));
      } catch (Throwable handlerThrowable) {
        resultFuture.completeExceptionally(handlerThrowable);
      }
    } else {
      resultFuture.completeExceptionally(actualThrowable);
    }
  }

  class WithFinalizer<X> implements Stream<X>, StreamContext<X> {
    private final Function<W, X> finalizer;

    WithFinalizer(Function<W, X> finalizer) {
      this.finalizer = finalizer;
    }

    @Override
    public RunnableStream<X> on(Runner runner) {
      return new RunnableStreamDefault<>(runner, this);
    }

    public StreamDefault<V, W> getStream() {
      return StreamDefault.this;
    }

    @Override
    public AtomicReference<X> getResult() {
      return null;
    }

    @Override
    public void onComplete(CompletableFuture<X> resultFuture) {
      try {
        X finalized = finalizer.apply(StreamDefault.this.result.get());
        resultFuture.complete(finalized);
      } catch (Throwable throwable) {
        resultFuture.completeExceptionally(throwable);
      }
    }

    @Override
    public void onError(CompletableFuture<X> result, Throwable throwable) {
      CompletableFuture<W> resultFuture = new CompletableFuture<>();
      StreamDefault.this.onError(resultFuture, throwable);
      resultFuture.whenComplete(
          (w, throwable1) -> {
            if (Objects.nonNull(throwable1)) {
              result.completeExceptionally(throwable1);
            } else {
              try {
                X finalized = finalizer.apply(w);
                result.complete(finalized);
              } catch (Throwable throwable2) {
                result.completeExceptionally(throwable2);
              }
            }
          });
    }
  }

  private static class RunnableStreamDefault<X> implements RunnableStream<X> {

    private final Runner runner;
    private final Stream<X> stream;

    RunnableStreamDefault(Runner runner, Stream<X> stream) {
      this.runner = runner;
      this.stream = stream;
    }

    @Override
    public CompletionStage<X> run() {
      return runner.run(stream);
    }
  }
}
