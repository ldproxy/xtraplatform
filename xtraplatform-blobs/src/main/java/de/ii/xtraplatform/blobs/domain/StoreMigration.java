/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.domain;

import de.ii.xtraplatform.base.domain.Migration;
import de.ii.xtraplatform.base.domain.StoreDriver;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.StoreMigration.StoreMigrationContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface StoreMigration extends Migration<StoreMigrationContext, StoreSourceFs> {
  enum Type {
    BLOB,
    EVENT
  }

  interface StoreMigrationContext extends MigrationContext {
    BlobReader reader();
  }

  Type getType();

  List<Tuple<StoreSourceFs, StoreSourceFs>> getMoves();

  List<Tuple<Path, Boolean>> getCleanups();

  default boolean isApplicable() {
    return isApplicable(null);
  }

  @Override
  default boolean isApplicable(StoreSourceFs source) {
    return getMoves().stream()
        .anyMatch(
            move -> {
              try {
                return getContext().reader().has(Path.of(move.first().getSrc()));
              } catch (IOException e) {
                return false;
              }
            });
  }

  default List<Tuple<Path, Boolean>> getActualCleanups() {
    return getCleanups().stream()
        .filter(
            cleanup -> {
              try {
                return getContext().reader().has(cleanup.first());
              } catch (IOException e) {
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  static String filters(StoreSourceFs source) {
    return filters(source, "");
  }

  static String filters(StoreSourceFs source, String replace) {
    if (!source.getIncludes().isEmpty()) {
      return "/" + source.getIncludes().get(0);
    }
    if (!source.getExcludes().isEmpty()) {
      return source.getExcludes().stream()
          .map(ex -> "[!/" + (replace.isEmpty() ? ex : ex.replace(replace + "/", "")) + "]")
          .collect(Collectors.joining());
    }

    return "";
  }

  default List<Map.Entry<String, String>> getPreview() {
    return getMoves().stream().flatMap(this::getPreviewForMove).collect(Collectors.toList());
  }

  default Stream<Map.Entry<String, String>> getPreviewForMove(
      Tuple<StoreSourceFs, StoreSourceFs> moves) {
    Content content = moves.first().getContent();

    if (content == Content.ENTITIES) {
      Path from = Path.of(moves.first().getSrc());
      return getPreviewForEntities(from, content);
    }
    if (content == Content.RESOURCES || content == Content.VALUES) {
      Path from = Path.of(moves.first().getSrc());
      Optional<String> prefix = moves.first().getPrefix();
      return getPreviewForResourcesOrValues(moves, from, content, prefix);
    }
    return Stream.<Map.Entry<String, String>>empty();
  }

  default Stream<Map.Entry<String, String>> getPreviewForEntities(Path from, Content content) {
    Path to = Path.of(content.getPrefix());
    List<Map.Entry<String, String>> entries = new ArrayList<>();

    addPreviewEntryIfExists(entries, from, to, Content.DEFAULTS);
    addPreviewEntryIfExists(entries, from, to, Content.INSTANCES_OLD, Content.INSTANCES);
    addPreviewEntryIfExists(entries, from, to, Content.OVERRIDES);

    return entries.stream();
  }

  default void addPreviewEntryIfExists(
      List<Map.Entry<String, String>> entries, Path from, Path to, Content sourceContent) {
    addPreviewEntryIfExists(entries, from, to, sourceContent, sourceContent);
  }

  default void addPreviewEntryIfExists(
      List<Map.Entry<String, String>> entries,
      Path from,
      Path to,
      Content sourceContent,
      Content targetContent) {
    try {
      if (getContext().reader().has(from.resolve(sourceContent.getPrefix()))) {
        String fromPath = from.resolve(sourceContent.getPrefix()).normalize().toString();
        String toPath = to.resolve(targetContent.getPrefix()).normalize().toString();

        if (sourceContent == Content.INSTANCES_OLD) {
          fromPath += filters(getMoves().get(0).first(), Content.INSTANCES_OLD.getPrefix());
        }

        entries.add(Map.entry(fromPath, toPath));
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions during preview - migration will handle missing sources gracefully
    }
  }

  default Stream<Map.Entry<String, String>> getPreviewForResourcesOrValues(
      Tuple<StoreSourceFs, StoreSourceFs> moves,
      Path from,
      Content content,
      Optional<String> prefix) {
    try {
      if (!getContext().reader().has(from)) {
        return Stream.<Map.Entry<String, String>>empty();
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions - source may not exist during preview
      return Stream.<Map.Entry<String, String>>empty();
    }

    String fromPath =
        moves.first().getAbsolutePath(Path.of("")).normalize().toString() + filters(moves.first());
    String toPath =
        moves
            .second()
            .getAbsolutePath(Path.of(content.getPrefix()))
            .resolve(Path.of(prefix.orElse("")))
            .normalize()
            .toString();

    return Stream.of(Map.entry(fromPath, toPath));
  }

  default List<Map.Entry<Path, Path>> migrate() {
    return getMoves().stream().flatMap(this::migrateMove).collect(Collectors.toList());
  }

  default Stream<Map.Entry<Path, Path>> migrateMove(Tuple<StoreSourceFs, StoreSourceFs> moves) {
    Content content = moves.first().getContent();

    if (content == Content.ENTITIES) {
      Path from = Path.of(moves.first().getSrc());
      return migrateEntities(moves, from);
    }
    if (content == Content.VALUES) {
      Path from = Path.of(moves.first().getSrc());
      Optional<String> prefix = moves.first().getPrefix();
      return migrateValues(moves, from, prefix);
    }
    if (content == Content.RESOURCES) {
      Path from = Path.of(moves.first().getSrc());
      Optional<String> prefix = moves.first().getPrefix();
      return migrateResources(moves, from, prefix);
    }
    return Stream.<Map.Entry<Path, Path>>empty();
  }

  default Stream<Map.Entry<Path, Path>> migrateEntities(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from) {
    Content content = moves.first().getContent();
    Path to = Path.of(content.getPrefix());
    List<Map.Entry<Path, Path>> entries = new ArrayList<>();

    addMigrationEntryIfExists(entries, from, to, Content.DEFAULTS);
    addInstancesMigration(entries, moves, from, to);
    addMigrationEntryIfExists(entries, from, to, Content.OVERRIDES);

    return entries.stream();
  }

  default void addMigrationEntryIfExists(
      List<Map.Entry<Path, Path>> entries, Path from, Path to, Content content) {
    try {
      if (getContext().reader().has(from.resolve(content.getPrefix()))) {
        entries.add(
            Map.entry(
                from.resolve(content.getPrefix()).normalize(),
                to.resolve(content.getPrefix()).normalize()));
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions during migration
    }
  }

  default void addInstancesMigration(
      List<Map.Entry<Path, Path>> entries,
      Tuple<StoreSourceFs, StoreSourceFs> moves,
      Path from,
      Path to) {
    try {
      if (getContext().reader().has(from.resolve(Content.INSTANCES_OLD.getPrefix()))) {
        if (moves.first().getIncludes().isEmpty() && moves.first().getExcludes().isEmpty()) {
          entries.add(
              Map.entry(
                  from.resolve(Content.INSTANCES_OLD.getPrefix()).normalize(),
                  to.resolve(Content.INSTANCES.getPrefix()).normalize()));
        } else {
          addFilteredInstancesMigration(entries, moves, from, to);
        }
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions during migration
    }
  }

  default void addFilteredInstancesMigration(
      List<Map.Entry<Path, Path>> entries,
      Tuple<StoreSourceFs, StoreSourceFs> moves,
      Path from,
      Path to) {
    Path from2 = from.resolve(Content.INSTANCES_OLD.getPrefix());
    Path to2 = to.resolve(Content.INSTANCES.getPrefix());
    List<PathMatcher> includes =
        StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
    List<PathMatcher> excludes =
        StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

    try (Stream<Path> paths =
        getContext()
            .reader()
            .walk(
                from2,
                8,
                (p, a) ->
                    a.isValue()
                        && (includes.isEmpty()
                            || includes.stream()
                                .anyMatch(include -> include.matches(from2.resolve(p))))
                        && excludes.stream()
                            .noneMatch(exclude -> exclude.matches(from2.resolve(p))))) {
      paths.forEach(
          path ->
              entries.add(
                  Map.entry(from2.resolve(path).normalize(), to2.resolve(path).normalize())));
    } catch (Throwable e) {
      // Ignore I/O exceptions during filtered migration
    }
  }

  default Stream<Map.Entry<Path, Path>> migrateValues(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from, Optional<String> prefix) {
    try {
      if (!getContext().reader().has(from)) {
        return Stream.<Map.Entry<Path, Path>>empty();
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions - source may not exist
      return Stream.<Map.Entry<Path, Path>>empty();
    }

    Path to = Path.of(Content.VALUES.getPrefix()).resolve(Path.of(prefix.orElse("")));
    List<PathMatcher> includes =
        StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
    List<PathMatcher> excludes =
        StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

    List<Map.Entry<Path, Path>> entries = new ArrayList<>();
    try (Stream<Path> paths =
        getContext()
            .reader()
            .walk(
                from,
                8,
                (p, a) ->
                    a.isValue()
                        && (includes.isEmpty()
                            || includes.stream()
                                .anyMatch(include -> include.matches(from.resolve(p))))
                        && excludes.stream()
                            .noneMatch(exclude -> exclude.matches(from.resolve(p))))) {
      paths.forEach(
          path ->
              entries.add(
                  Map.entry(
                      from.resolve(path).normalize(),
                      to.resolve(
                              Objects.equals(prefix.orElse(""), "codelists")
                                  ? path.getFileName()
                                  : path)
                          .normalize())));
    } catch (Throwable e) {
      // Ignore I/O exceptions during values migration
    }
    return entries.stream();
  }

  default Stream<Map.Entry<Path, Path>> migrateResources(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from, Optional<String> prefix) {
    try {
      if (!getContext().reader().has(from)) {
        return Stream.<Map.Entry<Path, Path>>empty();
      }
    } catch (Throwable e) {
      // Ignore I/O exceptions - source may not exist
      return Stream.<Map.Entry<Path, Path>>empty();
    }

    if (prefix.isEmpty() || prefix.get().startsWith("tiles")) {
      return migrateResourcesTiles(moves, from, prefix);
    } else if (moves.first().getIncludes().isEmpty() && moves.first().getExcludes().isEmpty()) {
      return migrateResourcesSimple(moves, from, prefix);
    } else {
      return migrateResourcesFiltered(moves, from, prefix);
    }
  }

  default Stream<Map.Entry<Path, Path>> migrateResourcesTiles(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from, Optional<String> prefix) {
    Path to = Path.of(Content.RESOURCES.getPrefix()).resolve(Path.of(prefix.orElse("")));
    List<PathMatcher> includes =
        StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
    List<PathMatcher> excludes =
        StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

    List<Map.Entry<Path, Path>> entries = new ArrayList<>();
    try (Stream<Path> paths = getContext().reader().walk(from, 1, (p, a) -> !a.isValue())) {
      paths
          .skip(1)
          .filter(
              path ->
                  (includes.isEmpty()
                          || includes.stream()
                              .anyMatch(include -> include.matches(from.resolve(path))))
                      && excludes.stream().noneMatch(exclude -> exclude.matches(from.resolve(path)))
                      && !path.endsWith("__tmp__"))
          .forEach(
              path ->
                  entries.add(
                      Map.entry(from.resolve(path).normalize(), to.resolve(path).normalize())));
    } catch (Throwable e) {
      // Ignore I/O exceptions during tiles migration
    }
    return entries.stream();
  }

  default Stream<Map.Entry<Path, Path>> migrateResourcesFiltered(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from, Optional<String> prefix) {
    Path to = Path.of(Content.RESOURCES.getPrefix()).resolve(Path.of(prefix.orElse("")));
    List<PathMatcher> includes =
        StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
    List<PathMatcher> excludes =
        StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

    List<Map.Entry<Path, Path>> entries = new ArrayList<>();
    try (Stream<Path> paths =
        getContext()
            .reader()
            .walk(
                from,
                8,
                (p, a) ->
                    a.isValue()
                        && (includes.isEmpty()
                            || includes.stream()
                                .anyMatch(include -> include.matches(from.resolve(p))))
                        && excludes.stream()
                            .noneMatch(exclude -> exclude.matches(from.resolve(p))))) {
      paths.forEach(
          path ->
              entries.add(Map.entry(from.resolve(path).normalize(), to.resolve(path).normalize())));
    } catch (Throwable e) {
      // Ignore I/O exceptions during filtered resources migration
    }
    return entries.stream();
  }

  default Stream<Map.Entry<Path, Path>> migrateResourcesSimple(
      Tuple<StoreSourceFs, StoreSourceFs> moves, Path from, Optional<String> prefix) {
    Path from2 = moves.first().getAbsolutePath(Path.of("")).normalize();
    Path to =
        moves
            .second()
            .getAbsolutePath(Path.of(Content.RESOURCES.getPrefix()))
            .resolve(Path.of(prefix.orElse("")))
            .normalize();

    return Stream.of(Map.entry(from2, to));
  }
}
