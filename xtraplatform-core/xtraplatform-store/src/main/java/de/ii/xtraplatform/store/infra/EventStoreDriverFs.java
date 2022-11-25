/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import static com.google.common.io.Files.getFileExtension;
import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.sun.nio.file.SensitivityWatchEventModifier;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.store.app.EventPaths;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.Identifier;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EventStoreDriverFs implements EventStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverFs.class);
  private static final String STORE_DIR_LEGACY = "config-store";

  private final Path storeDirectory;
  private final List<Path> additionalDirectories;
  private final EventPaths eventPaths;
  private final List<EventPaths> additionalEventPaths;
  private final boolean isEnabled;
  private final boolean isReadOnly;

  @Inject
  EventStoreDriverFs(AppContext appContext) {
    this(appContext.getDataDir(), appContext.getConfiguration().store);
  }

  public EventStoreDriverFs(Path dataDirectory, StoreConfiguration storeConfiguration) {
    this.storeDirectory = getStoreDirectory(dataDirectory, storeConfiguration);
    this.eventPaths = new EventPaths(storeDirectory, this::adjustPathPattern);
    this.isEnabled = true; // TODO: storeConfiguration.driver = StoreDriver.FS
    this.isReadOnly = storeConfiguration.isReadOnly();

    this.additionalDirectories = getAdditionalDirectories(dataDirectory, storeConfiguration);
    this.additionalEventPaths =
        additionalDirectories.stream()
            .map(
                additionalDirectory -> new EventPaths(additionalDirectory, this::adjustPathPattern))
            .collect(Collectors.toList());
  }

  private String adjustPathPattern(String pattern) {
    return pattern.replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator());
  }

  @Override
  public void start() {
    if (!isEnabled) return;

    if (!Files.exists(storeDirectory) && isReadOnly) {
      throw new IllegalArgumentException(
          "Store path does not exist and cannot be created because store is read-only");
    }

    LOGGER.info("Store location: {}", storeDirectory.toAbsolutePath());

    if (!additionalEventPaths.isEmpty()) {
      LOGGER.info(
          "Additional store locations: {}",
          additionalEventPaths.stream()
              .map(EventPaths::getRootPath)
              .map(Path::toAbsolutePath)
              .collect(Collectors.toList()));
    }

    try {
      boolean usingNewStore =
          Files.isDirectory(storeDirectory) && Files.list(storeDirectory).findFirst().isPresent();
      Files.createDirectories(storeDirectory);

      Path legacyStoreDirectory = storeDirectory.getParent().resolve(STORE_DIR_LEGACY);
      boolean usingLegacyStore =
          Files.isDirectory(legacyStoreDirectory)
              && Files.list(legacyStoreDirectory).findFirst().isPresent();

      if (usingLegacyStore) {
        if (usingNewStore) {
          LOGGER.warn(
              "Found non-empty stores in '{}' and '{}'. Please merge manually and remove '{}'.",
              legacyStoreDirectory.toAbsolutePath(),
              storeDirectory.toAbsolutePath(),
              legacyStoreDirectory.toAbsolutePath());
        } else {
          migrateStore(legacyStoreDirectory);
        }
      }
    } catch (IOException e) {

    }
  }

  @Override
  public Stream<EntityEvent> loadEventStream() {
    if (!isEnabled) return Stream.<EntityEvent>empty();

    // TODO
    /*Path pkgDir = storeDirectory.getParent().resolve("pkgs");
    try {
      de.ii.xtraplatform.store.app.xpk.XpkReader xpkReader = new de.ii.xtraplatform.store.app.xpk.XpkReader();
      xpkReader.readPackages(pkgDir, (path, payload) -> LOGGER.error("PKG ENTRY {}", path));
    } catch (Throwable e) {
      // ignore
      LOGGER.error("", e);
    }*/

    try {
      return Stream.concat(
          eventPaths
              .getPathPatternStream()
              .flatMap(
                  pathPattern ->
                      loadPathStream(storeDirectory)
                          .map(
                              path ->
                                  eventPaths.pathToEvent(
                                      pathPattern, path, this::readPayload, false))
                          .filter(Objects::nonNull))
              .sorted(Comparator.naturalOrder()),
          additionalEventPaths.stream()
              .filter(additionalEventPath -> Files.exists(additionalEventPath.getRootPath()))
              .flatMap(
                  additionalEventPath ->
                      additionalEventPath
                          .getPathPatternStream()
                          .flatMap(
                              pathPattern ->
                                  loadPathStream(additionalEventPath.getRootPath())
                                      .map(
                                          path ->
                                              additionalEventPath.pathToEvent(
                                                  pathPattern, path, this::readPayload, true))
                                      .filter(Objects::nonNull))
                          .sorted(Comparator.naturalOrder())));

    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Reading events from '{}' failed", storeDirectory);
    }

    return Stream.empty();
  }

  @Override
  public boolean supportsWatch() {
    return true;
  }

  // TODO: stopWatching, move watchService to class, watch new directories, file extension filter
  @Override
  public void startWatching(Consumer<List<Path>> watchEventConsumer) {
    if (!isEnabled) return;

    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      final Map<WatchKey, List<Path>> keys = new HashMap<>();

      keys.putAll(watchDirectory(watchService, storeDirectory));

      for (Path additionalDirectory : additionalDirectories) {
        keys.putAll(watchDirectory(watchService, additionalDirectory));
      }

      WatchKey key;
      while ((key = watchService.take()) != null) {
        if (!keys.containsKey(key)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WatchKey " + key + " not recognized!");
          }
          continue;
        }
        final Path rootDir = keys.get(key).get(0);
        final Path watchDir = keys.get(key).get(1);
        List<Path> changedFiles =
            key.pollEvents().stream()
                .filter(watchEvent -> watchEvent.context() instanceof Path)
                .filter(
                    watchEvent -> {
                      String fileExtension = getFileExtension(watchEvent.context().toString());
                      // TODO: either inject from store or filter at a later stage
                      return Objects.equals(fileExtension, "yml")
                          || Objects.equals(fileExtension, "yaml")
                          || Objects.equals(fileExtension, "json");
                    })
                .map(
                    watchEvent -> rootDir.relativize(watchDir.resolve((Path) watchEvent.context())))
                .collect(Collectors.toList());

        if (!changedFiles.isEmpty()) {
          watchEventConsumer.accept(changedFiles);
        }

        key.reset();
      }
    } catch (IOException | InterruptedException e) {
      LogContext.error(LOGGER, e, "Could not watch directory {}", storeDirectory);
    }
  }

  private Map<WatchKey, List<Path>> watchDirectory(WatchService watchService, Path rootDir)
      throws IOException {
    final Map<WatchKey, List<Path>> keys = new HashMap<>();

    Files.walkFileTree(
        rootDir,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            WatchKey watchKey =
                dir.register(
                    watchService,
                    new WatchEvent.Kind[] {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE},
                    SensitivityWatchEventModifier.HIGH);
            keys.put(watchKey, ImmutableList.of(rootDir, dir));
            return FileVisitResult.CONTINUE;
          }
        });

    return keys;
  }

  private Stream<Path> loadPathStream(Path directory) {
    try {
      return Files.find(
          directory, 32, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile());
    } catch (IOException e) {
      throw new IllegalStateException("Reading event from store path failed", e);
    }
  }

  private byte[] readPayload(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new IllegalStateException("Reading event from file failed", e);
    }
  }

  @Override
  public void saveEvent(EntityEvent event) throws IOException {
    // TODO: check mainPath first, if exists use override
    // TODO: if override exists, merge with incoming
    Path eventPath = eventPaths.getSavePath(event);
    /*if (Files.exists(eventPath)) {
        eventPath = getEventFilePath(event.type(), event.identifier(), event.format(), savePathPattern);
    }*/
    Files.createDirectories(eventPath.getParent());
    Files.write(eventPath, event.payload());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Saved event to file {}", eventPath);
    }
  }

  // TODO: only delete overrides if migration
  @Override
  public void deleteAllEvents(String type, Identifier identifier, String format)
      throws IOException {
    for (Path path : eventPaths.getDeletePaths(type, identifier, format)) {
      deleteEvent(path);
    }
  }

  private void deleteEvent(Path eventPath) throws IOException {
    if (!Files.isDirectory(eventPath.getParent())) {
      return;
    }

    // TODO: better error handling
    Files.list(eventPath.getParent())
        .forEach(
            consumerMayThrow(
                file -> {
                  if (Files.isRegularFile(file)
                      && (Objects.equals(eventPath, file)
                          || file.getFileName()
                              .toString()
                              .startsWith(eventPath.getFileName().toString() + "."))) {
                    String fileName = file.getFileName().toString();
                    String name = file.toFile().getName();
                    Path backup;
                    if (file.getParent().endsWith("#overrides#")) {
                      backup = file.getParent().getParent().resolve(".backup/#overrides#");
                    } else {
                      backup = file.getParent().resolve(".backup");
                    }
                    Files.createDirectories(backup);
                    Files.copy(file, backup.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(file);
                    if (LOGGER.isDebugEnabled()) {
                      LOGGER.debug("Deleted event file {}", eventPath);
                    }

                    if (file.getParent().endsWith("#overrides#")) {
                      try {
                        Files.delete(file.getParent());
                      } catch (Throwable e) {
                        // ignore
                      }
                    }

                    boolean stop = true;
                  }
                }));
  }

  private Path getStoreDirectory(Path dataDir, StoreConfiguration storeConfiguration) {
    Path storeLocation = Paths.get(storeConfiguration.getLocation());
    if (storeLocation.isAbsolute()) {
      if (storeConfiguration.isReadWrite() && !storeLocation.startsWith(dataDir)) {
        // not allowed?
        throw new IllegalStateException(
            String.format(
                "Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).",
                storeLocation, dataDir));
      }
      return storeLocation;
    }

    return dataDir.resolve(storeLocation);
  }

  private List<Path> getAdditionalDirectories(Path dataDir, StoreConfiguration storeConfiguration) {
    ImmutableList.Builder<Path> additionalDirectories = new ImmutableList.Builder<>();

    for (String additionalLocation : storeConfiguration.getAdditionalLocations()) {
      Path storeLocation = Paths.get(additionalLocation);
      if (storeLocation.isAbsolute()) {
        if (storeConfiguration.isReadWrite() && !storeLocation.startsWith(dataDir)) {
          // not allowed?
          throw new IllegalStateException(
              String.format(
                  "Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).",
                  storeLocation, dataDir));
        }
        additionalDirectories.add(storeLocation);
      } else {
        additionalDirectories.add(dataDir.resolve(storeLocation));
      }
    }

    return additionalDirectories.build();
  }

  private void migrateStore(Path legacyStoreDirectory) {
    try {
      List<Path> directoriesToDelete = new ArrayList<>();

      Files.walk(legacyStoreDirectory)
          .forEach(
              fileOrDirectory -> {
                try {
                  Path newFileOrDirectory =
                      storeDirectory.resolve(legacyStoreDirectory.relativize(fileOrDirectory));
                  if (Files.isDirectory(fileOrDirectory)) {
                    if (Files.list(fileOrDirectory).findFirst().isPresent()) {
                      LOGGER.debug("Creating directory {}", newFileOrDirectory);
                      Files.createDirectories(newFileOrDirectory);
                    }
                    directoriesToDelete.add(0, fileOrDirectory);
                  } else {
                    LOGGER.debug("Copying File {}", newFileOrDirectory);
                    Files.copy(
                        fileOrDirectory, newFileOrDirectory); // use flag to override existing
                    Files.delete(fileOrDirectory);
                  }
                } catch (Exception e) {
                  throw new IllegalStateException(e.getMessage());
                }
              });

      for (Path path : directoriesToDelete) {
        Files.delete(path);
      }

      LOGGER.info(
          "Migrated store from '{}' to '{}'",
          legacyStoreDirectory.toAbsolutePath(),
          storeDirectory.toAbsolutePath());
    } catch (Throwable e) {
      LogContext.error(
          LOGGER,
          e,
          "Error migrating store from '{}' to '{}': {}",
          legacyStoreDirectory.toAbsolutePath(),
          storeDirectory.toAbsolutePath());
    }
  }
}
