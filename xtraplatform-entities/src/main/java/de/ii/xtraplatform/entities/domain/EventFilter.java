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
import java.util.Set;
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
        return EntityDataOverridesPath.isTypeOverride(identifier)
            || getIds().contains(identifier.path().get(identifier.path().size() - 1));
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

  default boolean isRestrictedToIds() {
    return !getIds().isEmpty() && !getIds().contains(WILDCARD);
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
    return fromPath(path, Set.of());
  }

  /**
   * @param path a store path like {@code entities/[groups/]type/id.yml}
   * @param entityTypes the known entity types, used to tell groups from the entity type; without
   *     them, the second path segment is assumed to be the entity type
   */
  static EventFilter fromPath(Path path, Set<String> entityTypes) {
    if (path.getNameCount() < 2) {
      return null;
    }

    String eventType = path.getName(0).toString();
    int typeIndex = indexOfEntityType(path, entityTypes);
    String entityType = withoutExtension(path.getName(typeIndex).toString());

    ImmutableEventFilter.Builder builder =
        ImmutableEventFilter.builder().addEventTypes(eventType).addEntityTypes(entityType);
    if ("defaults".equals(eventType)) {
      builder.addIds(WILDCARD);
    } else if (path.getNameCount() > typeIndex + 1 && !changesTypeOverride(path, typeIndex)) {
      builder.addIds(withoutExtension(path.getName(typeIndex + 1).toString()));
    }

    return builder.build();
  }

  private static int indexOfEntityType(Path path, Set<String> entityTypes) {
    for (int i = 1; i < path.getNameCount(); i++) {
      if (entityTypes.contains(withoutExtension(path.getName(i).toString()))) {
        return i;
      }
    }
    return 1;
  }

  private static String withoutExtension(String segment) {
    int dotIdx = segment.indexOf('.');
    return dotIdx == -1 ? segment : segment.substring(0, dotIdx);
  }

  // a changed type override affects every entity of the type, so all of them have to be reloaded
  private static boolean changesTypeOverride(Path path, int typeIndex) {
    for (int i = typeIndex + 1; i < path.getNameCount(); i++) {
      if (EntityDataOverridesPath.isTypeOverrideSegment(path.getName(i).toString())) {
        return true;
      }
    }
    return false;
  }

  static EventFilter fromPaths(List<Path> paths, Set<String> entityTypes) {
    ImmutableEventFilter.Builder builder = ImmutableEventFilter.builder();

    paths.stream()
        .map(path -> fromPath(path, entityTypes))
        .filter(Objects::nonNull)
        .forEach(builder::from);

    return builder.build();
  }
}
