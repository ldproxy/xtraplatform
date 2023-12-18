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
import java.util.Map.Entry;
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
    return getMoves().stream()
        .flatMap(
            moves -> {
              Content content = moves.first().getContent();
              Path from = Path.of(moves.first().getSrc());
              Optional<String> prefix = moves.first().getPrefix();

              if (content == Content.ENTITIES) {
                Path to = Path.of(content.getPrefix());
                List<Entry<String, String>> inter = new ArrayList<>();

                try {
                  if (getContext().reader().has(from.resolve(Content.DEFAULTS.getPrefix()))) {
                    inter.add(
                        Map.entry(
                            from.resolve(Content.DEFAULTS.getPrefix()).normalize().toString(),
                            to.resolve(Content.DEFAULTS.getPrefix()).normalize().toString()));
                  }
                } catch (Throwable e) {
                }
                try {
                  if (getContext().reader().has(from.resolve(Content.INSTANCES_OLD.getPrefix()))) {
                    inter.add(
                        Map.entry(
                            from.resolve(Content.INSTANCES_OLD.getPrefix()).normalize().toString()
                                + filters(moves.first(), Content.INSTANCES_OLD.getPrefix()),
                            to.resolve(Content.INSTANCES.getPrefix()).normalize().toString()));
                  }
                } catch (Throwable e) {
                }
                try {
                  if (getContext().reader().has(from.resolve(Content.OVERRIDES.getPrefix()))) {
                    inter.add(
                        Map.entry(
                            from.resolve(Content.OVERRIDES.getPrefix()).normalize().toString(),
                            to.resolve(Content.OVERRIDES.getPrefix()).normalize().toString()));
                  }
                } catch (Throwable e) {
                }

                return inter.stream();
              }
              if (content == Content.RESOURCES || content == Content.VALUES) {
                try {
                  if (!getContext().reader().has(from)) {
                    return Stream.<Entry<String, String>>empty();
                  }
                } catch (Throwable e) {
                  // ignore
                  System.out.println("E " + e.getMessage());
                  return Stream.<Entry<String, String>>empty();
                }

                String from2 =
                    moves.first().getAbsolutePath(Path.of("")).normalize().toString()
                        + filters(moves.first());
                String to =
                    moves
                        .second()
                        .getAbsolutePath(Path.of(content.getPrefix()))
                        .resolve(Path.of(prefix.orElse("")))
                        .normalize()
                        .toString();

                return Stream.of(Map.entry(from2, to));
              }
              return Stream.<Entry<String, String>>empty();
            })
        .collect(Collectors.toList());
  }

  default List<Map.Entry<Path, Path>> migrate() {
    return getMoves().stream()
        .flatMap(
            moves -> {
              Content content = moves.first().getContent();
              Path from = Path.of(moves.first().getSrc());
              Optional<String> prefix = moves.first().getPrefix();

              if (content == Content.ENTITIES) {
                Path to = Path.of(content.getPrefix());
                List<Entry<Path, Path>> inter = new ArrayList<>();

                try {
                  if (getContext().reader().has(from.resolve(Content.DEFAULTS.getPrefix()))) {
                    inter.add(
                        Map.entry(
                            from.resolve(Content.DEFAULTS.getPrefix()).normalize(),
                            to.resolve(Content.DEFAULTS.getPrefix()).normalize()));
                  }
                } catch (Throwable e) {
                }
                try {
                  if (getContext().reader().has(from.resolve(Content.INSTANCES_OLD.getPrefix()))) {
                    if (!moves.first().getIncludes().isEmpty()
                        || !moves.first().getExcludes().isEmpty()) {
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
                                                  .anyMatch(
                                                      include -> include.matches(from2.resolve(p))))
                                          && excludes.stream()
                                              .noneMatch(
                                                  exclude -> exclude.matches(from2.resolve(p))))) {
                        paths.forEach(
                            path ->
                                inter.add(
                                    Map.entry(
                                        from2.resolve(path).normalize(),
                                        to2.resolve(path).normalize())));
                      } catch (Throwable e) {
                        // ignore
                        System.out.println("E " + e.getMessage());
                      }
                    } else {
                      inter.add(
                          Map.entry(
                              from.resolve(Content.INSTANCES_OLD.getPrefix()).normalize(),
                              to.resolve(Content.INSTANCES.getPrefix()).normalize()));
                    }
                  }
                } catch (Throwable e) {
                }
                try {
                  if (getContext().reader().has(from.resolve(Content.OVERRIDES.getPrefix()))) {
                    inter.add(
                        Map.entry(
                            from.resolve(Content.OVERRIDES.getPrefix()).normalize(),
                            to.resolve(Content.OVERRIDES.getPrefix()).normalize()));
                  }
                } catch (Throwable e) {
                }

                return inter.stream();
              }
              if (content == Content.VALUES) {
                try {
                  if (!getContext().reader().has(from)) {
                    return Stream.<Entry<Path, Path>>empty();
                  }
                } catch (Throwable e) {
                  // ignore
                  System.out.println("E " + e.getMessage());
                  return Stream.<Entry<Path, Path>>empty();
                }

                Path to = Path.of(content.getPrefix()).resolve(Path.of(prefix.orElse("")));
                List<PathMatcher> includes =
                    StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
                List<PathMatcher> excludes =
                    StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

                List<Entry<Path, Path>> inter = new ArrayList<>();
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
                          inter.add(
                              Map.entry(
                                  from.resolve(path).normalize(),
                                  to.resolve(
                                          Objects.equals(prefix.orElse(""), "codelists")
                                              ? path.getFileName()
                                              : path)
                                      .normalize())));
                } catch (Throwable e) {
                  // ignore
                  System.out.println("E " + e.getMessage());
                }
                return inter.stream();
              }
              if (content == Content.RESOURCES) {
                try {
                  if (!getContext().reader().has(from)) {
                    return Stream.<Entry<Path, Path>>empty();
                  }
                } catch (Throwable e) {
                  // ignore
                  System.out.println("E " + e.getMessage());
                  return Stream.<Entry<Path, Path>>empty();
                }

                if (prefix.isEmpty() || prefix.get().startsWith("tiles")) {
                  Path to = Path.of(content.getPrefix()).resolve(Path.of(prefix.orElse("")));
                  List<PathMatcher> includes =
                      StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
                  List<PathMatcher> excludes =
                      StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

                  List<Entry<Path, Path>> inter = new ArrayList<>();
                  try (Stream<Path> paths =
                      getContext().reader().walk(from, 1, (p, a) -> !a.isValue())) {
                    paths
                        .skip(1)
                        .filter(
                            path ->
                                (includes.isEmpty()
                                        || includes.stream()
                                            .anyMatch(
                                                include -> include.matches(from.resolve(path))))
                                    && excludes.stream()
                                        .noneMatch(exclude -> exclude.matches(from.resolve(path)))
                                    && !path.endsWith("__tmp__"))
                        // .sorted(Comparator.reverseOrder())
                        .forEach(
                            path ->
                                inter.add(
                                    Map.entry(
                                        from.resolve(path).normalize(),
                                        to.resolve(path).normalize())));
                  } catch (Throwable e) {
                    // ignore
                    System.out.println("E " + e.getMessage());
                  }
                  return inter.stream();
                } else if (!moves.first().getIncludes().isEmpty()
                    || !moves.first().getExcludes().isEmpty()) {
                  Path to = Path.of(content.getPrefix()).resolve(Path.of(prefix.orElse("")));
                  List<PathMatcher> includes =
                      StoreDriver.asMatchers(moves.first().getIncludes(), from.toString());
                  List<PathMatcher> excludes =
                      StoreDriver.asMatchers(moves.first().getExcludes(), from.toString());

                  List<Entry<Path, Path>> inter = new ArrayList<>();
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
                                              .anyMatch(
                                                  include -> include.matches(from.resolve(p))))
                                      && excludes.stream()
                                          .noneMatch(
                                              exclude -> exclude.matches(from.resolve(p))))) {
                    paths.forEach(
                        path ->
                            inter.add(
                                Map.entry(
                                    from.resolve(path).normalize(), to.resolve(path).normalize())));
                  } catch (Throwable e) {
                    // ignore
                    System.out.println("E " + e.getMessage());
                  }
                  return inter.stream();
                }

                Path from2 = moves.first().getAbsolutePath(Path.of("")).normalize();
                Path to =
                    moves
                        .second()
                        .getAbsolutePath(Path.of(content.getPrefix()))
                        .resolve(Path.of(prefix.orElse("")))
                        .normalize();

                return Stream.of(Map.entry(from2, to));
              }
              return Stream.<Entry<Path, Path>>empty();
            })
        .collect(Collectors.toList());
  }
}
