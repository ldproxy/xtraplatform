package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;

public class SourceTransformed<T, U> implements Source<U> {

  private final SourceDefault<T> source;
  private final Transformer<T, U> transformer;

  public SourceTransformed(SourceDefault<T> source, Transformer<T, U> transformer) {
    this.source = source;
    this.transformer = transformer;
  }

  @Override
  public <U1> Source<U1> via(Transformer<U, U1> transformer) {
    if (isFuseable(transformer)) {
      return new SourceTransformed<>(source, fuse(this.transformer, transformer));
    }
    return new SourceTransformed<>(source, new TransformerChained<>(this.transformer, transformer));
  }

  @Override
  public <V> BasicStream<U, V> to(Sink<U, V> sink) {
    return new StreamDefault<>(this, sink);
  }

  public SourceDefault<T> getSource() {
    return source;
  }

  public Transformer<T, U> getTransformer() {
    return transformer;
  }

  private <U1> boolean isFuseable(Transformer<U, U1> transformer) {
    return this.transformer instanceof TranformerCustomFuseableOut && transformer instanceof TranformerCustomFuseableIn
        && ((TranformerCustomFuseableOut<T, U, ?>) this.transformer).canFuse((TranformerCustomFuseableIn<U, U1, ?>) transformer);
  }

  private <U1, V> Transformer<T, U1> fuse(Transformer<T, U> transformer1, Transformer<U, U1> transformer2) {
    TranformerCustomFuseableOut<T, U, V> out = (TranformerCustomFuseableOut<T, U, V>) transformer1;
    TranformerCustomFuseableIn<U, U1, V> in = (TranformerCustomFuseableIn<U, U1, V>) transformer2;

    return new TransformerFused<>(out, in);
  }
}
