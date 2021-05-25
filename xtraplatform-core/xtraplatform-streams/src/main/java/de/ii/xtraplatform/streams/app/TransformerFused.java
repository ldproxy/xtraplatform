package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustom;
import java.util.function.Consumer;

public class TransformerFused<T, U, V, W> implements TransformerCustom<T, V> {

  private final TransformerCustom<T, U> transformer1;
  private final TransformerCustom<U, V> transformer2;

  public TransformerFused(TranformerCustomFuseableOut<T, U, W> transformer1,
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
}
