package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.Transformer;

public class TransformerChained<T, U, V> implements Transformer<T, V> {

  private final Transformer<T, U> transformer1;
  private final Transformer<U, V> transformer2;

  public TransformerChained(
      Transformer<T, U> transformer1,
      Transformer<U, V> transformer2) {
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
