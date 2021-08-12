/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.Transformer;

public class TransformerChained<T, U, V> implements Transformer<T, V> {

  private final Transformer<T, U> transformer1;
  private final Transformer<U, V> transformer2;

  public TransformerChained(Transformer<T, U> transformer1, Transformer<U, V> transformer2) {
    this.transformer1 = transformer1;
    this.transformer2 = transformer2;
  }

  public Transformer<T, U> getTransformer1() {
    return transformer1;
  }

  public Transformer<U, V> getTransformer2() {
    return transformer2;
  }
}
