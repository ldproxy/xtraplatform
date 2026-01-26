/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.values.domain.Identifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
public interface EventFilter {

  String WILDCARD = "*";

  List<String> getEventTypes();

  List<String> getEntityTypes();

  List<String> getIds();

  @SuppressWarnings("PMD.CyclomaticComplexity")
  default boolean matches(EntityEvent event) {
    Identifier identifier = event.identifier();

    if (!getEntityTypes().contains(WILDCARD)
        && (identifier.path().isEmpty() || !containsEntityType(identifier.path()))
        && (!isDefault(event) || !containsEntityType(identifier.id()))) {
      return false;
    }

    if (!getIds().isEmpty() && !isDefault(event)) {
      boolean allow = getIds().contains(identifier.id()) || getIds().contains(WILDCARD);

      if (!allow && isOverride(event)) {
        return getIds().contains(identifier.path().get(identifier.path().size() - 1));
      }

      return allow;
    }

    return true;
  }

  default boolean matches(Identifier identifier) {
    if (!getEntityTypes().contains(WILDCARD)
        && (identifier.path().isEmpty() || !containsEntityType(identifier.path()))
        && !getEntityTypes().contains(identifier.id())) {
      return false;
    }

    if (!getIds().isEmpty()) {
      return getIds().contains(identifier.id()) || getIds().contains(WILDCARD);
    }

    return true;
  }

  default boolean isDefault(EntityEvent event) {
    return Objects.equals(event.type(), EntityDataDefaultsStore.EVENT_TYPE);
  }

  default boolean isOverride(EntityEvent event) {
    return Objects.equals(event.type(), EntityDataStore.EVENT_TYPE_OVERRIDES);
  }

  default boolean containsEntityType(List<String> path) {
    return indexOfEntityType(path) > -1;
  }

  default int indexOfEntityType(List<String> path) {
    for (int i = 0; i < path.size(); i++) {
      if (getEntityTypes().contains(path.get(i))) {
        return i;
      }
    }

    return -1;
  }

  default boolean containsEntityType(String id) {
    if (getEntityTypes().contains(id)) {
      return true;
    } else if (id.indexOf('.') != -1) {
      String entityType = id.substring(0, id.indexOf('.'));
      if (getEntityTypes().contains(entityType)) {
        return true;
      }
    }

    return false;
  }

  static EventFilter fromPath(Path path) {
    if (path.getNameCount() < 2) {
      return null;
    }

    String eventType = path.getName(0).toString();
    String entityType = path.getName(1).toString();
    int dotIdx = entityType.indexOf('.');
    if (dotIdx != -1) {
      entityType = entityType.substring(0, dotIdx);
    }

    ImmutableEventFilter.Builder builder =
        ImmutableEventFilter.builder().addEventTypes(eventType).addEntityTypes(entityType);
    // TODO
    if ("defaults".equals(eventType)) {
      builder.addIds(WILDCARD);
    } else if (path.getNameCount() > 2) {
      String id = path.getName(2).toString();
      if (id.contains(".")) {
        id = id.substring(0, id.indexOf('.'));
      }
      builder.addIds(id);
    }

    return builder.build();
  }

  static EventFilter fromPaths(List<Path> paths) {
    ImmutableEventFilter.Builder builder = ImmutableEventFilter.builder();

    paths.stream().map(EventFilter::fromPath).filter(Objects::nonNull).forEach(builder::from);

    return builder.build();
  }
}
