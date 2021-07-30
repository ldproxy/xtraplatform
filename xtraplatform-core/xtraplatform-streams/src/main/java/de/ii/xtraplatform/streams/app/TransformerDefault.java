/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class TransformerDefault<T, U> implements Transformer<T, U> {

  public enum Type {
    MAP,
    PEEK,
    REDUCE
  }

  private final Type type;
  private final Function<T, U> function;
  private final Consumer<T> consumer;
  private final U item;
  private final BiFunction<U, T, U> reducer;

  public TransformerDefault(Function<T, U> function) {
    this(Type.MAP, function, null, null, null);
  }

  public TransformerDefault(Consumer<T> consumer) {
    this(Type.PEEK, null, consumer, null, null);
  }

  public TransformerDefault(U item, BiFunction<U, T, U> reducer) {
    this(Type.REDUCE, null, null, item, reducer);
  }

  TransformerDefault(
      Type type,
      Function<T, U> function,
      Consumer<T> consumer,
      U item,
      BiFunction<U, T, U> reducer) {
    this.type = type;
    this.function = function;
    this.consumer = consumer;
    this.item = item;
    this.reducer = reducer;
  }

  @Override
  public <V> Transformer<T, V> via(Transformer<U, V> transformer) {
    return new TransformerChained<>(this, transformer);
  }

  public Type getType() {
    return type;
  }

  public Function<T, U> getFunction() {
    return function;
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
}
