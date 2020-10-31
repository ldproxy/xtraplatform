/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.store.domain.Identifier;
import java.util.List;
import org.immutables.value.Value;

// TODO: unit tests for all cases
@Value.Modifiable
public interface EntityDataDefaultsPath {

  Splitter DOT_SPLITTER = Splitter.on('.');

  static EntityDataDefaultsPath from(Identifier identifier) {
    ModifiableEntityDataDefaultsPath defaultsPath = ModifiableEntityDataDefaultsPath.create();
    List<String> pathSegments;

    if (identifier.path().isEmpty()) {
      if (identifier.id().contains(".")) {
        int firstDot = identifier.id().indexOf(".");
        defaultsPath.setEntityType(identifier.id().substring(0, firstDot));
        pathSegments = DOT_SPLITTER.splitToList(identifier.id().substring(firstDot + 1));
      } else {
        defaultsPath.setEntityType(identifier.id());
        pathSegments = ImmutableList.of();
      }
    } else {
      defaultsPath.setEntityType(identifier.path().get(0));
      pathSegments = identifier.path().subList(1, identifier.path().size());
    }

    //TODO: describe cases, how would catch happen?
    if (!pathSegments.isEmpty()) {
      //for (int i = pathSegments.size(); i > 0; i--) {
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
      //}
    }

    return defaultsPath;
  }

  String getEntityType();

  List<String> getEntitySubtype();

  List<String> getKeyPath();
}
