/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ImmutableIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

// TODO: unit tests for all cases
@Value.Modifiable
public interface EntityDataDefaultsPath {

  Splitter DOT_SPLITTER = Splitter.on('.');

  static EntityDataDefaultsPath from(Identifier identifier, Set<String> entityTypes) {
    ModifiableEntityDataDefaultsPath defaultsPath = ModifiableEntityDataDefaultsPath.create();

    boolean found = false;
    List<String> pathSegments = new ArrayList<>();

    for (int i = 0; i < identifier.path().size(); i++) {
      String segment = identifier.path().get(i);

      if (entityTypes.contains(segment)) {
        found = true;
        defaultsPath.setEntityType(segment);
        if (i > 0) {
          defaultsPath.setGroups(identifier.path().subList(0, i));
        }
        pathSegments.clear();
        if (identifier.path().size() > i + 1) {
          pathSegments.addAll(identifier.path().subList(i + 1, identifier.path().size()));
        }
        continue;
      }

      if (!segment.contains(".")) {
        continue;
      }

      int firstDot = segment.indexOf('.');
      String beforeDot = segment.substring(0, firstDot);
      if (!entityTypes.contains(beforeDot)) {
        continue;
      }

      found = true;
      defaultsPath.setEntityType(beforeDot);
      pathSegments.clear();
      pathSegments.addAll(DOT_SPLITTER.splitToList(segment.substring(firstDot + 1)));
      if (i > 0) {
        defaultsPath.setGroups(identifier.path().subList(0, i));
      }
      if (identifier.path().size() > i + 1) {
        pathSegments.addAll(identifier.path().subList(i + 1, identifier.path().size()));
      }
    }

    if (!found) {
      if (identifier.id().contains(".")) {
        int firstDot = identifier.id().indexOf(".");
        defaultsPath.setEntityType(identifier.id().substring(0, firstDot));
        pathSegments = DOT_SPLITTER.splitToList(identifier.id().substring(firstDot + 1));
      } else {
        defaultsPath.setEntityType(identifier.id());
      }
      defaultsPath.setGroups(identifier.path());
    }

    // TODO: describe cases, how would catch happen?
    if (!pathSegments.isEmpty()) {
      try {
        List<String> subtype = pathSegments.subList(0, pathSegments.size());
        defaultsPath.setEntitySubtype(ImmutableList.of(Joiner.on('/').join(subtype)));

        if (!identifier.path().isEmpty()) {
          defaultsPath.setKeyPath(ImmutableList.of(identifier.id()));
        }
      } catch (Throwable e) {
        List<String> keyPath = pathSegments.subList(pathSegments.size() - 1, pathSegments.size());

        defaultsPath.setKeyPath(keyPath);
        defaultsPath.addKeyPath(identifier.id());
      }
      // }
    }

    return defaultsPath;
  }

  List<String> getGroups();

  String getEntityType();

  List<String> getEntitySubtype();

  List<String> getKeyPath();

  @Value.Lazy
  default Identifier asIdentifier() {
    return ImmutableIdentifier.builder()
        .addAllPath(Lists.reverse(getGroups()))
        .addPath(getEntityType())
        .addAllPath(getEntitySubtype())
        .id(EntityDataDefaultsStore.EVENT_TYPE)
        .build();
  }
}
