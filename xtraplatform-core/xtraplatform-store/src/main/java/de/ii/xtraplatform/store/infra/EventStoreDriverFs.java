/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import static com.google.common.io.Files.getFileExtension;
import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.consumerMayThrow;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.google.common.collect.ImmutableList;
import com.sun.nio.file.SensitivityWatchEventModifier;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.runtime.domain.Constants;
import de.ii.xtraplatform.runtime.domain.StoreConfiguration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class EventStoreDriverFs implements EventStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverFs.class);
  private static final String STORE_DIR_LEGACY = "config-store";

  @ServiceController(value = false)
  private boolean publish;

  private final Path storeDirectory;
  private final EventPaths eventPaths;
  private final List<EventPaths> additionalEventPaths;
  private final boolean isEnabled;
  private final boolean isReadOnly;

  EventStoreDriverFs(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform) {
    this.storeDirectory =
        getStoreDirectory(
            bundleContext.getProperty(Constants.DATA_DIR_KEY),
            xtraPlatform.getConfiguration().store);
    this.eventPaths =
        new EventPaths(
            storeDirectory,
            xtraPlatform.getConfiguration().store.instancePathPattern,
            xtraPlatform.getConfiguration().store.overridesPathPatterns,
            this::adjustPathPattern);
    this.isEnabled = true; // TODO: xtraPlatform.getConfiguration().store.driver = StoreDriver.FS
    this.isReadOnly =
        xtraPlatform.getConfiguration().store.mode == StoreConfiguration.StoreMode.READ_ONLY;

    List<Path> additionalDirectories =
        getAdditionalDirectories(
            bundleContext.getProperty(Constants.DATA_DIR_KEY),
            xtraPlatform.getConfiguration().store);
    this.additionalEventPaths =
        additionalDirectories.stream()
            .map(
                additionalDirectory ->
                    new EventPaths(
                        additionalDirectory,
                        xtraPlatform.getConfiguration().store.instancePathPattern,
                        xtraPlatform.getConfiguration().store.overridesPathPatterns,
                        this::adjustPathPattern))
            .collect(Collectors.toList());
  }

  private String adjustPathPattern(String pattern) {
    return pattern.replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator());
  }

  @Validate
  private void onInit() {
    if (!Files.exists(storeDirectory) && isReadOnly) {
      throw new IllegalArgumentException(
          "Store path does not exist and cannot be created because store is read-only");
    }
    if (isEnabled) {
      this.publish = true;
    }
  }

  @Override
  public void start() {
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
    try {
      return Stream.concat(
          eventPaths
              .getPathPatternStream()
              .flatMap(
                  pathPattern ->
                      loadPathStream(storeDirectory)
                          .map(path -> eventPaths.pathToEvent(pathPattern, path, this::readPayload))
                          .filter(Objects::nonNull)),
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
                                                  pathPattern, path, this::readPayload))
                                      .filter(Objects::nonNull))));

    } catch (Throwable e) {
      LOGGER.error("Reading events from '{}' failed: {}", storeDirectory, e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace:", e);
      }
    }

    return Stream.empty();
  }

  @Override
  public boolean supportsWatch() {
    return true;
  }

  // TODO: stopWatching, move watchService to class, watch new directories, file extension filter
  @Override
  public void startWatching(Consumer<Path> watchEventConsumer) {

    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      final Map<WatchKey, Path> keys = new HashMap<>();

      Files.walkFileTree(
          storeDirectory,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              // LOGGER.debug("registering " + dir + " in watcher service");
              WatchKey watchKey =
                  dir.register(
                      watchService,
                      new WatchEvent.Kind[] {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE},
                      SensitivityWatchEventModifier.HIGH);
              keys.put(watchKey, dir);
              return FileVisitResult.CONTINUE;
            }
          });
      // LOGGER.debug("Watching directory for changes {}", storeDirectory);
      WatchKey key;
      while ((key = watchService.take()) != null) {
        final Path dir = keys.get(key);
        if (dir == null) {
          LOGGER.error("WatchKey " + key + " not recognized!");
          continue;
        }
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.context() instanceof Path) {
            String fileExtension = getFileExtension(event.context().toString());
            if (Objects.equals(fileExtension, "yml")
                || Objects.equals(fileExtension, "yaml")
                || Objects.equals(fileExtension, "json")) {
              Path file = dir.resolve((Path) event.context());
              // LOGGER.debug("FILE {}", storeDirectory.relativize(file));
              watchEventConsumer.accept(storeDirectory.relativize(file));
            }
          }
        }
        key.reset();
      }
    } catch (IOException | InterruptedException e) {
      LOGGER.error("Could not watch directory {}: {}", storeDirectory, e.getMessage());
    }
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

  private Path getStoreDirectory(String dataDir, StoreConfiguration storeConfiguration) {
    String storeLocation = storeConfiguration.location;
    if (Paths.get(storeLocation).isAbsolute()) {
      if (storeConfiguration.mode == StoreConfiguration.StoreMode.READ_WRITE
          && !storeLocation.startsWith(dataDir)) {
        // not allowed?
        throw new IllegalStateException(
            String.format(
                "Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).",
                storeLocation, dataDir));
      }
      return Paths.get(storeLocation);
    }

    return Paths.get(dataDir, storeLocation);
  }

  private List<Path> getAdditionalDirectories(
      String dataDir, StoreConfiguration storeConfiguration) {
    ImmutableList.Builder<Path> additionalDirectories = new ImmutableList.Builder<>();

    for (String storeLocation : storeConfiguration.additionalLocations) {
      if (Paths.get(storeLocation).isAbsolute()) {
        if (storeConfiguration.mode == StoreConfiguration.StoreMode.READ_WRITE
            && !storeLocation.startsWith(dataDir)) {
          // not allowed?
          throw new IllegalStateException(
              String.format(
                  "Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).",
                  storeLocation, dataDir));
        }
        additionalDirectories.add(Paths.get(storeLocation));
      } else {
        additionalDirectories.add(Paths.get(dataDir, storeLocation));
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
      LOGGER.error(
          "Error migrating store from '{}' to '{}': {}",
          legacyStoreDirectory.toAbsolutePath(),
          storeDirectory.toAbsolutePath(),
          e.getMessage());
    }
  }
}
