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
import de.ii.xtraplatform.base.domain.StoreSource.Mode;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.store.app.EventSource;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.EventStoreDriver.Watcher;
import de.ii.xtraplatform.store.domain.EventStoreDriver.Writer;
import de.ii.xtraplatform.store.domain.Identifier;
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
public class EventStoreDriverFs implements EventStoreDriver, Watcher, Writer {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverFs.class);

  private final Path dataDirectory;
  private final EventReader eventReaderDir;
  private final EventReader eventReaderZip;

  @Inject
  EventStoreDriverFs(AppContext appContext) {
    this(appContext.getDataDir());
  }

  public EventStoreDriverFs(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    this.eventReaderDir = new EventReaderDir();
    this.eventReaderZip = new EventReaderZip();
  }

  @Override
  public Type getType() {
    return Type.FS;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    EventSource source = from(storeSource);

    return storeSource.isArchive()
        ? Files.isRegularFile(source.getPath())
        : Files.isDirectory(source.getPath());
  }

  @Override
  public Stream<EntityEvent> load(StoreSource storeSource) {
    EventSource source = from(storeSource);

    if (!Files.exists(source.getPath()) && !source.getPath().startsWith(dataDirectory)) {
      LOGGER.warn("Store source {} not found.", source.getSource().getLabel());
      return Stream.empty();
    }
    if (!storeSource.isArchive() && !Files.isDirectory(source.getPath())) {
      LOGGER.warn("Store source {} is not a directory.", source.getSource().getLabel());
      return Stream.empty();
    }
    if (storeSource.isArchive() && !Files.isRegularFile(source.getPath())) {
      LOGGER.warn("Store source {} is not an archive.", source.getSource().getLabel());
      return Stream.empty();
    }

    EventReader eventReader = storeSource.isArchive() ? eventReaderZip : eventReaderDir;

    return source.load(eventReader);
  }

  // TODO: stopWatching, move watchService to class, watch new directories, file extension filter
  @Override
  public void listen(StoreSource storeSource, Consumer<List<Path>> watchEventConsumer) {
    EventSource source = from(storeSource);

    if (!source.getSource().isWatchable() || source.getSource().isArchive()) {
      LOGGER.warn("Watching is disabled for source {}.", source.getSource().getLabel());
      return;
    }

    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      final Map<WatchKey, List<Path>> keys = new HashMap<>();

      try {
        keys.putAll(watchDirectory(watchService, source.getPath()));

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Watching source {}.", source.getSource().getLabel());
        }
      } catch (IOException e) {
        LogContext.error(LOGGER, e, "Cannot watch source {}", source.getSource().getLabel());
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

  @Override
  public void push(StoreSource storeSource, EntityEvent event) throws IOException {
    EventSource source = from(storeSource);

    if (source.getSource().getMode() == Mode.RO) {
      LOGGER.warn("Writing is disabled for source {}.", source.getSource().getLabel());
      return;
    }

    // TODO: check mainPath first, if exists use override
    // TODO: if override exists, merge with incoming
    Path eventPath = source.getSavePath(event);
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
  public void deleteAll(StoreSource storeSource, String type, Identifier identifier, String format)
      throws IOException {
    EventSource source = from(storeSource);

    if (source.getSource().getMode() == Mode.RO) {
      LOGGER.warn("Writing is disabled for source {}.", source.getSource().getLabel());
      return;
    }

    for (Path path : source.getDeletePaths(type, identifier, format)) {
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
    return new EventSource(getAbsolutePath(dataDirectory, source), source, this::adjustPathPattern);
  }

  private String adjustPathPattern(String pattern) {
    return pattern.replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator());
  }

  private static Path getAbsolutePath(Path dataDir, StoreSource storeSource) {
    Path src = Path.of(storeSource.getSrc());
    return src.isAbsolute() ? src : dataDir.resolve(src);
  }
}
