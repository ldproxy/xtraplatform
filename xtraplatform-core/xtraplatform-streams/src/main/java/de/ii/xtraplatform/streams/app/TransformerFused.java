/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReducedTransformed;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.function.Consumer;

public class TransformerFused<T, U, V, W> implements TranformerCustomFuseableOut<T, V, W> {

  private final TranformerCustomFuseableOut<T, U, W> transformer1;
  private final TransformerCustomFuseableIn<U, V, W> transformer2;

  public TransformerFused(
      TranformerCustomFuseableOut<T, U, W> transformer1,
      TransformerCustomFuseableIn<U, V, W> transformer2) {
    this.transformer1 = transformer1;
    this.transformer2 = transformer2;
    transformer1.fuse(transformer2);
  }

  @Override
  public void init(Consumer<V> push) {
    transformer2.init(push);
  }

  @Override
  public void onPush(T t) {
    transformer1.onPush(t);
  }

  @Override
  public void onComplete() {
    transformer1.onComplete();
    transformer2.onComplete();
  }

  @Override
  public Class<? extends W> getFusionInterface() {
    return transformer1.getFusionInterface();
  }

  @Override
  public void fuse(TransformerCustomFuseableIn<V, ?, ? extends W> transformerCustomFuseableIn) {
    if (!canFuse(transformerCustomFuseableIn)) {
      throw new IllegalArgumentException();
    }
    ((TranformerCustomFuseableOut<U, V, W>) transformer2).fuse(transformerCustomFuseableIn);
  }

  @Override
  public boolean canFuse(TransformerCustomFuseableIn<V, ?, ?> transformerCustomFuseableIn) {
    return transformer2 instanceof TranformerCustomFuseableOut
        && ((TranformerCustomFuseableOut<U, V, W>) transformer2)
            .canFuse(transformerCustomFuseableIn);
  }

  @Override
  public <V1> Transformer<T, V1> via(Transformer<V, V1> transformer) {
    if (transformer instanceof TransformerCustomFuseableIn && canFuse(
        (TransformerCustomFuseableIn<V, ?, ?>) transformer)) {
      return new TransformerFused<>(this, (TransformerCustomFuseableIn<V, V1, W>) transformer);
    }
    if (transformer instanceof TransformerChained) {
      return via((TransformerChained<V, ?, V1>)transformer);
    }

    return new TransformerChained<>(this, transformer);
  }

  public <V1, W1> Transformer<T, V1> via(TransformerChained<V, W1, V1> transformer) {
    Transformer<V, W1> transformer1 = transformer.getTransformer1();
    if (transformer1 instanceof TransformerCustomFuseableIn && canFuse(
        (TransformerCustomFuseableIn<V, W1, ?>) transformer1)) {
      Transformer<W1, V1> transformer2 = transformer.getTransformer2();
      Transformer<T, W1> via = via(transformer1);
      new TransformerChained<>(via, transformer2);
    }

    return new TransformerChained<>(this, transformer);
  }

  @Override
  public <X> SinkReducedTransformed<T, V, X> to(SinkReduced<V, X> sink) {
    if (sink instanceof SinkTransformedImpl && ((SinkTransformedImpl<V, ?, X>) sink).getTransformer() instanceof TransformerCustomFuseableIn) {
      TransformerCustomFuseableIn<V, ?, W> x = x(((SinkTransformedImpl<V, ?, X>) sink).getTransformer());
      if (canFuse(x)) {
        fuse(x);
      }
    }

    return TranformerCustomFuseableOut.super.to(sink);
  }

  @Override
  public SinkTransformed<T, V> to(Sink<V> sink) {
    return TranformerCustomFuseableOut.super.to(sink);
  }

  private <X> TransformerCustomFuseableIn<V, X, W> x(Transformer<V, X> transformer) {
    return (TransformerCustomFuseableIn<V, X, W>) transformer;
  }
}
