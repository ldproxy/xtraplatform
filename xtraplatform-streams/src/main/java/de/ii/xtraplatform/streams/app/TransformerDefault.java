/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TransformerDefault<T, U> implements Transformer<T, U> {

  public enum Type {
    MAP,
    FILTER,
    PEEK,
    REDUCE,
    FLATMAP
  }

  private final Type type;
  private final Function<T, U> function;
  private final Predicate<T> predicate;
  private final Consumer<T> consumer;
  private final U item;
  private final BiFunction<U, T, U> reducer;
  private final Function<T, Source<U>> flatMap;
  private Source<U> prepend;
  private Source<U> mergeSorted;
  private Comparator<U> mergeSortedComparator;

  public TransformerDefault(Function<T, U> function) {
    this(Type.MAP, function, null, null, null, null, null);
  }

  public TransformerDefault(Function<T, Source<U>> function, boolean flatMap) {
    this(Type.FLATMAP, null, null, null, null, null, function);
  }

  public TransformerDefault(Predicate<T> predicate) {
    this(Type.FILTER, null, predicate, null, null, null, null);
  }

  public TransformerDefault(Consumer<T> consumer) {
    this(Type.PEEK, null, null, consumer, null, null, null);
  }

  public TransformerDefault(U item, BiFunction<U, T, U> reducer) {
    this(Type.REDUCE, null, null, null, item, reducer, null);
  }

  TransformerDefault(
      Type type,
      Function<T, U> function,
      Predicate<T> predicate,
      Consumer<T> consumer,
      U item,
      BiFunction<U, T, U> reducer,
      Function<T, Source<U>> flatMap) {
    this.type = type;
    this.function = function;
    this.predicate = predicate;
    this.consumer = consumer;
    this.item = item;
    this.reducer = reducer;
    this.flatMap = flatMap;
  }

  @Override
  public <V> Transformer<T, V> via(Transformer<U, V> transformer) {
    return new TransformerChained<>(this, transformer);
  }

  @Override
  public Transformer<T, U> prepend(Source<U> other) {
    this.prepend = other;

    return this;
  }

  @Override
  public Transformer<T, U> mergeSorted(Source<U> other, Comparator<U> comparator) {
    this.mergeSorted = other;
    this.mergeSortedComparator = comparator;

    return this;
  }

  public Type getType() {
    return type;
  }

  public Function<T, U> getFunction() {
    return function;
  }

  public Predicate<T> getPredicate() {
    return predicate;
  }

  public Consumer<T> getConsumer() {
    return consumer;
  }

  public U getItem() {
    return item;
  }

  public BiFunction<U, T, U> getReducer() {
    return reducer;
  }

  public Function<T, Source<U>> getFlatMap() {
    return flatMap;
  }

  public Optional<Source<U>> getPrepend() {
    return Optional.ofNullable(prepend);
  }

  public Optional<Source<U>> getMergeSorted() {
    return Optional.ofNullable(mergeSorted);
  }

  public Optional<Comparator<U>> getMergeSortedComparator() {
    return Optional.ofNullable(mergeSortedComparator);
  }
}
