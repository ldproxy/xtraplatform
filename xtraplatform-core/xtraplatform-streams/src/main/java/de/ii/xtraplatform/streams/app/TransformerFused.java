/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustom;
import java.util.function.Consumer;

public class TransformerFused<T, U, V, W> implements TranformerCustomFuseableOut<T, V, W> {

  private final TranformerCustomFuseableOut<T, U, W> transformer1;
  private final TransformerCustom<U, V> transformer2;

  public TransformerFused(
      TranformerCustomFuseableOut<T, U, W> transformer1,
      TranformerCustomFuseableIn<U, V, W> transformer2) {
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
  public void fuse(TranformerCustomFuseableIn<V, ?, ? extends W> tranformerCustomFuseableIn) {
    if (!canFuse(tranformerCustomFuseableIn)) {
      throw new IllegalArgumentException();
    }
    ((TranformerCustomFuseableOut<U, V, W>) transformer2).fuse(tranformerCustomFuseableIn);
  }

  @Override
  public boolean canFuse(TranformerCustomFuseableIn<V, ?, ?> tranformerCustomFuseableIn) {
    return transformer2 instanceof TranformerCustomFuseableOut
        && ((TranformerCustomFuseableOut<U, V, W>) transformer2)
            .canFuse(tranformerCustomFuseableIn);
  }
}
