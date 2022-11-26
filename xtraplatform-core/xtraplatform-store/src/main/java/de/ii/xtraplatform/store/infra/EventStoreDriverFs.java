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
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.store.app.EventSource;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.Store;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EventStoreDriverFs
    implements EventStoreDriver, EventStoreDriver.Watch, EventStoreDriver.Write {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverFs.class);
  private static final String TYPE = "FS";

  private final List<EventSource> sources;
  private final Optional<EventSource> writableSource;
  private final Path dataDirectory;
  private final boolean isEnabled;

  @Inject
  EventStoreDriverFs(AppContext appContext, Store storeSources) {
    this(appContext.getDataDir(), storeSources);
  }

  public EventStoreDriverFs(Path dataDirectory, Store storeSources) {
    this.dataDirectory = dataDirectory;
    this.sources = storeSources.get(Type.FS, this::from);
    this.writableSource = storeSources.getWritable(Type.FS, this::from);
    this.isEnabled = !sources.isEmpty();
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void start() {
    if (!isEnabled) return;

    for (EventSource source : sources) {
      if (!Files.exists(source.getRootPath()) && !source.getRootPath().startsWith(dataDirectory)) {
        LOGGER.warn("Store source {} not found.", source.getSource().getShortLabel());
      }
    }
  }

  @Override
  public Stream<EntityEvent> loadEventStream() {
    if (!isEnabled) return Stream.empty();

    return sources.stream()
        .filter(pathParser -> Files.exists(pathParser.getRootPath()))
        .flatMap(
            pathParser ->
                pathParser
                    .getPathPatternStream()
                    .flatMap(
                        pathPattern -> {
                          try {
                            return loadPathStream(pathParser.getRootPath())
                                .map(
                                    path ->
                                        pathParser.pathToEvent(
                                            pathPattern, path, this::readPayload))
                                .filter(Objects::nonNull);
                          } catch (Throwable e) {
                            LogContext.error(
                                LOGGER,
                                e,
                                "Loading {} failed.",
                                pathParser.getSource().getShortLabel());
                          }
                          return Stream.empty();
                        })
                    .sorted(Comparator.naturalOrder())); // );
  }

  @Override
  public boolean canWatch() {
    return true;
  }

  // TODO: stopWatching, move watchService to class, watch new directories, file extension filter
  @Override
  public void start(Consumer<List<Path>> watchEventConsumer) {
    if (!isEnabled) return;

    List<EventSource> watchSources =
        sources.stream()
            .filter(source -> source.getSource().isWatchable())
            .filter(source -> Files.isDirectory(source.getRootPath()))
            .collect(Collectors.toList());

    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      final Map<WatchKey, List<Path>> keys = new HashMap<>();

      for (EventSource source : watchSources) {
        try {
          keys.putAll(watchDirectory(watchService, source.getRootPath()));

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Watching source {}.", source.getSource().getShortLabel());
          }
        } catch (IOException e) {
          LogContext.error(LOGGER, e, "Cannot watch source {}", source.getSource().getShortLabel());
        }
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
      LogContext.error(LOGGER, e, "Could not watch store.");
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
  public void push(EntityEvent event) throws IOException {
    if (writableSource.isEmpty()) {
      LOGGER.warn("Ignoring write event for '{}', no writable source found.", event.asPath());
      return;
    }

    // TODO: check mainPath first, if exists use override
    // TODO: if override exists, merge with incoming
    Path eventPath = writableSource.get().getSavePath(event);
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
  public void deleteAll(String type, Identifier identifier, String format) throws IOException {
    if (writableSource.isEmpty()) {
      LOGGER.warn("Ignoring delete event for '{}', no writable source found.", identifier.asPath());
      return;
    }

    for (Path path : writableSource.get().getDeletePaths(type, identifier, format)) {
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

  private EventSource from(StoreSource source) {
    return new EventSource(
        getRootDirectory(dataDirectory, source), source, this::adjustPathPattern);
  }

  private String adjustPathPattern(String pattern) {
    return pattern.replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator());
  }

  private static Path getRootDirectory(Path dataDir, StoreSource storeSource) {
    Path src = Path.of(storeSource.getSrc());
    return src.isAbsolute() ? src : dataDir.resolve(src);
  }
}
