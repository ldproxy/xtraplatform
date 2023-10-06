/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain.maptobuilder.encoding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import de.ii.xtraplatform.entities.domain.maptobuilder.ImmutableBuildableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.immutables.encode.Encoding;

@Encoding
class BuildableMapEncoding<T extends Buildable<T>, U extends BuildableBuilder<T>> {
  @Encoding.Impl private BuildableMap<T, U> field;

  @Encoding.Expose
  BuildableMap<T, U> getImmutableTable() {
    return field; // <-- this is how our accessor would be implemented
  }

  @Encoding.Builder // <-- put annotation
  static class Builder<
      T extends Buildable<T>,
      U extends BuildableBuilder<T>> { // <-- copy type parameters from the encoding

    // private ImmutableValueOrBuilderMap.Builder<T, U> buildValue = new
    // ImmutableValueOrBuilderMap.Builder<T, U>();
    private Map<String, U> builderMap = new LinkedHashMap<>();

    @JsonIgnore
    @Encoding.Init // <-- specify builder initializer method
    @Encoding.Copy // <-- marks it as "canonical" copy method
    public void set(Map<String, T> values) {
      // this.buildValue = new ImmutableValueOrBuilderMap.Builder<T, U>();//.from(value);
      // values.forEach(this::put);
      this.builderMap = new LinkedHashMap<>();
      values.forEach(this::put);
    }

    @JsonProperty
    @Encoding.Naming(value = "get*")
    public Map<String, U> get() {
      // return buildValue.build().getBuilders();
      return builderMap;
    }

    @Encoding.Init
    @Encoding.Naming(standard = Encoding.StandardNaming.PUT, depluralize = true)
    void put(String key, T value) {
      // TODO
      // buildValue.putBuilders(key, value.toBuilder());
      builderMap.put(key, (U) value.getBuilder());
    }

    @Encoding.Init
    @Encoding.Naming(standard = Encoding.StandardNaming.PUT, depluralize = true)
    void putBuilder(String key, U builder) {
      // here, table builder handles checks for us
      // buildValue.putBuilders(key, builder);
      builderMap.put(key, builder);
    }

    @Encoding.Init
    @Encoding.Naming(standard = Encoding.StandardNaming.PUT_ALL, depluralize = true)
    void putAll(Map<String, T> values) {
      // TODO
      values.forEach(this::put);
    }

    @JsonProperty
    @Encoding.Init
    @Encoding.Naming(depluralize = true, value = "set*")
    void putAllBuilders(Map<String, U> builders) {
      // here, table builder handles checks for us
      // buildValue.putAllBuilders(builders);
      builderMap.putAll(builders);
    }

    @Encoding.Build
    BuildableMap<T, U> build() {
      // return buildValue.build();
      return new ImmutableBuildableMap.Builder<T, U>().builders(builderMap).build();
    }
  }
}
