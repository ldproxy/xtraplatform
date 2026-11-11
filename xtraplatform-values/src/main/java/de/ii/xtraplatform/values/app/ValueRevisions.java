/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.KeyValueStore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Revisions of the values in a store, overall and per value type. */
class ValueRevisions {

  private final Supplier<Collection<String>> valueTypes;
  private final Map<String, AtomicLong> revisions;
  private final AtomicLong revision;

  ValueRevisions(Supplier<Collection<String>> valueTypes) {
    this.valueTypes = valueTypes;
    this.revisions = new ConcurrentHashMap<>();
    this.revision = new AtomicLong();
  }

  long get() {
    return revision.get();
  }

  long get(List<String> valueType) {
    if (Objects.isNull(valueType)) {
      return 0;
    }
    AtomicLong typeRevision = revisions.get(String.join("/", valueType));
    return Objects.nonNull(typeRevision) ? typeRevision.get() : 0;
  }

  // to be called after the value has been stored, so that a new revision always sees the new value
  void onChange(Identifier identifier) {
    valueTypes.get().stream()
        .filter(type -> KeyValueStore.valueTypeMatches(identifier, type))
        .forEach(type -> revisions.computeIfAbsent(type, t -> new AtomicLong()).incrementAndGet());
    revision.incrementAndGet();
  }
}
