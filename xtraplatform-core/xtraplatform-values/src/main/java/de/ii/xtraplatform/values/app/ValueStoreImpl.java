/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.values.api.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.values.api.ValueDecoderWithBuilder;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.Builder;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.Value;
import de.ii.xtraplatform.values.domain.ValueCache;
import de.ii.xtraplatform.values.domain.ValueFactories;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ValueStoreImpl implements ValueStore<Value> {
  private final ValueFactories valueFactories;
  private final ValueCache<Value> cache;
  private final ValueEncodingJackson<Value> valueEncoding;

  @Inject
  public ValueStoreImpl(AppContext appContext, Jackson jackson, ValueFactories valueFactories) {
    this.valueFactories = valueFactories;
    this.valueEncoding =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().getStore().isFailOnUnknownProperties());
    this.cache =
        new ValueCache<Value>() {
          @Override
          public boolean isInCache(Identifier identifier) {
            return false;
          }

          @Override
          public boolean isInCache(Predicate<Identifier> keyMatcher) {
            return false;
          }

          @Override
          public Value getFromCache(Identifier identifier) {
            return null;
          }
        };

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(new ValueDecoderWithBuilder<>(this::getBuilder, cache));
  }

  protected Builder<Value> getBuilder(Identifier identifier) {
    return (Builder<Value>) valueFactories.get(ValueStore.valueType(identifier)).builder();
  }

  @Override
  public List<Identifier> identifiers(String... path) {
    return null;
  }

  @Override
  public boolean has(Identifier identifier) {
    return false;
  }

  @Override
  public boolean has(Predicate<Identifier> matcher) {
    return false;
  }

  @Override
  public Value get(Identifier identifier) {
    return null;
  }

  @Override
  public CompletableFuture<Value> put(Identifier identifier, Value value) {
    return null;
  }

  @Override
  public CompletableFuture<Boolean> delete(Identifier identifier) {
    return null;
  }
}
