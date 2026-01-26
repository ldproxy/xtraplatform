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
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.immutables.encode.Encoding;

@Encoding
class MergeableMapEncoding<T, U> {
  @Encoding.Impl private Map<T, U> field;

  @Encoding.Expose
  Map<T, U> getMergeableMap() {
    return field; // <-- this is how our accessor would be implemented
  }

  @Encoding.Builder // <-- put annotation
  static class Builder<T, U> { // <-- copy type parameters from the encoding

    private Map<T, U> mergeableMap = new LinkedHashMap<>();

    @JsonIgnore
    @Encoding.Init // <-- specify builder initializer method
    @Encoding.Copy // <-- marks it as "canonical" copy method
    public void set(Map<T, U> values) {
      this.mergeableMap = new LinkedHashMap<>();
      values.forEach(this::put);
    }

    @JsonProperty
    @Encoding.Naming("get*")
    public Map<T, U> get() {
      return mergeableMap;
    }

    @Encoding.Init
    @Encoding.Naming(standard = Encoding.StandardNaming.PUT, depluralize = true)
    void put(T key, U value) {
      mergeableMap.put(key, value);
    }

    @Encoding.Init
    @Encoding.Naming(standard = Encoding.StandardNaming.PUT_ALL, depluralize = true)
    void putAll(Map<T, U> values) {
      values.forEach(this::put);
    }

    @JsonProperty
    @Encoding.Init
    @Encoding.Naming("set*")
    void putAllJackson(Map<T, U> values) {
      values.forEach(this::put);
    }

    @Encoding.Build
    Map<T, U> build() {
      return ImmutableMap.copyOf(mergeableMap);
    }
  }
}
