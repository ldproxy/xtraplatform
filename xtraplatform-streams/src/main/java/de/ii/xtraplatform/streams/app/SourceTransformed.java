/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReducedTransformed;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomFuseableIn;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

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

  public <U1, W> Source<W> via(TransformerChained<U, U1, W> transformer) {
    Transformer<U, U1> transformer1 = transformer.getTransformer1();
    Transformer<U1, W> transformer2 = transformer.getTransformer2();

    Source<U1> via1 =
        transformer1 instanceof TransformerChained
            ? via((TransformerChained<U, ?, U1>) transformer1)
            : via(transformer1);

    Source<W> via2 =
        transformer2 instanceof TransformerChained
            ? via1.via((TransformerChained<U1, ?, W>) transformer2)
            : via1.via(transformer2);

    return via2;
  }

  @Override
  public <V> BasicStream<U, V> to(SinkReduced<U, V> sink) {
    return new StreamDefault<>(this, sink);
  }

  @Override
  public <V, W> BasicStream<V, W> to(SinkReducedTransformed<U, V, W> sink) {
    if (sink instanceof SinkTransformedImpl) {
      Transformer<U, V> transformer = ((SinkTransformedImpl<U, V, W>) sink).getTransformer();
      SinkReduced<V, W> sink1 = ((SinkTransformedImpl<U, V, W>) sink).getSink();

      if (transformer instanceof TransformerChained) {
        return via((TransformerChained<U, ?, V>) transformer).to(sink1);
      }

      return via(transformer).to(sink1);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Source<U> mapError(Function<Throwable, Throwable> errorMapper) {
    source.mapError(errorMapper);

    return this;
  }

  @Override
  public Source<U> prepend(Source<U> other) {
    transformer.prepend(other);

    return this;
  }

  @Override
  public Source<U> mergeSorted(Source<U> other, Comparator<U> comparator) {
    transformer.mergeSorted(other, comparator);

    return this;
  }

  public SourceDefault<T> getSource() {
    return source;
  }

  public Transformer<T, U> getTransformer() {
    return transformer;
  }

  private <U1> boolean isFuseable(Transformer<U, U1> transformer) {
    TranformerCustomFuseableOut<?, U, ?> fuseableOut =
        this.transformer instanceof TranformerCustomFuseableOut
            ? (TranformerCustomFuseableOut<T, U, ?>) this.transformer
            : this.transformer instanceof TransformerChained
                    && ((TransformerChained<T, ?, U>) this.transformer).getTransformer2()
                        instanceof TranformerCustomFuseableOut
                ? (TranformerCustomFuseableOut<?, U, ?>)
                    ((TransformerChained<T, ?, U>) this.transformer).getTransformer2()
                : null;

    return transformer instanceof TransformerCustomFuseableIn
        && Objects.nonNull(fuseableOut)
        && fuseableOut.canFuse((TransformerCustomFuseableIn<U, U1, ?>) transformer);
  }

  private <U1, U2, V> Transformer<T, U1> fuse(
      Transformer<T, U> transformer1, Transformer<U, U1> transformer2) {
    TransformerCustomFuseableIn<U, U1, V> in = (TransformerCustomFuseableIn<U, U1, V>) transformer2;

    if (transformer1 instanceof TranformerCustomFuseableOut) {
      TranformerCustomFuseableOut<T, U, V> out =
          (TranformerCustomFuseableOut<T, U, V>) transformer1;
      return new TransformerFused<>(out, in);
    }

    TransformerChained<T, U2, U> chained = (TransformerChained<T, U2, U>) transformer1;
    Transformer<T, U2> other = chained.getTransformer1();
    TranformerCustomFuseableOut<U2, U, V> out =
        (TranformerCustomFuseableOut<U2, U, V>) chained.getTransformer2();

    return new TransformerChained<>(other, new TransformerFused<>(out, in));
  }
}
