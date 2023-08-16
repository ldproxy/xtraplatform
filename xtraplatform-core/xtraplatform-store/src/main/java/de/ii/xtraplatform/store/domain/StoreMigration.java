/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.domain.StoreMigration.StoreMigrationContext;
import java.io.IOException;
import java.nio.file.Path;
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

  default List<Map.Entry<Path, Path>> migrate() {
    return getMoves().stream()
        .flatMap(
            moves -> {
              if (moves.first().getContent() == Content.ENTITIES) {
                Path from = Path.of(moves.first().getSrc());
                Path to = Path.of(Content.ENTITIES.getPrefix());
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
                    inter.add(
                        Map.entry(
                            from.resolve(Content.INSTANCES_OLD.getPrefix()).normalize(),
                            to.resolve(Content.INSTANCES.getPrefix()).normalize()));
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
              if (moves.first().getContent() == Content.RESOURCES) {
                Path from = Path.of(moves.first().getSrc());
                Optional<String> prefix = moves.first().getPrefix();

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
                  Path to =
                      Path.of(Content.RESOURCES.getPrefix()).resolve(Path.of(prefix.orElse("")));

                  List<Entry<Path, Path>> inter = new ArrayList<>();
                  try (Stream<Path> paths =
                      getContext().reader().walk(from, 1, (p, a) -> !a.isValue())) {
                    paths
                        .skip(1)
                        .filter(
                            path ->
                                !Objects.equals(to.resolve(path), Path.of("resources/resources"))
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
                }

                Path from2 = moves.first().getAbsolutePath(Path.of("")).normalize();
                Path to =
                    moves
                        .second()
                        .getAbsolutePath(Path.of(Content.RESOURCES.getPrefix()))
                        .resolve(Path.of(prefix.orElse("")))
                        .normalize();

                return Stream.of(Map.entry(from2, to));
              }
              return Stream.<Entry<Path, Path>>empty();
            })
        .collect(Collectors.toList());
  }
}
