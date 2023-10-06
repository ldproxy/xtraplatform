/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

// TODO: unit tests for all cases
@Value.Modifiable
public interface EntityDataOverridesPath {

  Splitter DOT_SPLITTER = Splitter.on('.');

  static EntityDataOverridesPath from(Identifier identifier, Set<String> entityTypes) {
    ModifiableEntityDataOverridesPath overridesPath = ModifiableEntityDataOverridesPath.create();

    List<String> pathSegments = new ArrayList<>();
    pathSegments.addAll(identifier.path());
    pathSegments.addAll(DOT_SPLITTER.splitToList(identifier.id()));

    if (pathSegments.size() < 2) {
      throw new IllegalArgumentException("Not a valid override path: " + identifier);
    }

    for (int i = 0; i < identifier.path().size(); i++) {
      if (entityTypes.contains(identifier.path().get(i))) {
        overridesPath.setEntityType(pathSegments.get(i));
        overridesPath.setEntityId(pathSegments.get(i + 1));
        if (i > 0) {
          overridesPath.setGroups(identifier.path().subList(0, i));
        }
        if (identifier.path().size() > i + 1) {
          overridesPath.setKeyPath(pathSegments.subList(i + 2, pathSegments.size()));
        }
      }
    }

    /*overridesPath.setEntityType(pathSegments.get(0));
    overridesPath.setEntityId(pathSegments.get(1));
    if (pathSegments.size() > 2) {
      overridesPath.setKeyPath(pathSegments.subList(2, pathSegments.size()));
    }*/

    return overridesPath;
  }

  List<String> getGroups();

  String getEntityType();

  String getEntityId();

  List<String> getKeyPath();

  @Value.Lazy
  default Identifier asIdentifier() {
    return ImmutableIdentifier.builder()
        .addAllPath(getGroups())
        .addPath(getEntityType())
        .id(getEntityId())
        .build();
  }
}
