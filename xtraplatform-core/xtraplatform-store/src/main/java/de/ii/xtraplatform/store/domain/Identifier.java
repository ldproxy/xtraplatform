/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(get = "*")
@JsonDeserialize(as = ImmutableIdentifier.class)
public interface Identifier extends Comparable<Identifier> {

  String id();

  List<String> path();

  static Identifier from(String id, String... path) {

    return ImmutableIdentifier.builder().id(id).addPath(path).build();
  }

  @Override
  default int compareTo(Identifier identifier) {

    for (int i = 0; i < path().size() && i < identifier.path().size(); i++) {
      int compared = path().get(i).compareTo(identifier.path().get(i));
      if (compared != 0) {
        return compared;
      }
    }

    int lengthDiff = path().size() - identifier.path().size();

    if (lengthDiff != 0) {
      return lengthDiff;
    }

    return id().compareTo(identifier.id());
  }
}
