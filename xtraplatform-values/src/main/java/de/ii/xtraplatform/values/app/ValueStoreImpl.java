/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.io.Files;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.Substitutions;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatile;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.blobs.domain.BlobStoreFactory;
import de.ii.xtraplatform.values.api.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.values.api.ValueDecoderWithBuilder;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.KeyValueStore;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueBuilder;
import de.ii.xtraplatform.values.domain.ValueCache;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import de.ii.xtraplatform.values.domain.ValueFactories;
import de.ii.xtraplatform.values.domain.ValueFactory;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.values.domain.ValueStoreDecorator;
import de.ii.xtraplatform.values.domain.Values;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = {ValueStore.class, AppLifeCycle.class})
public class ValueStoreImpl extends AbstractVolatile
    implements ValueStore, KeyValueStore<StoredValue>, ValueCache<StoredValue>, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueStoreImpl.class);

  private final BlobStore blobStore;
  private final ValueFactories valueFactories;
  private final Map<Identifier, StoredValue> memCache;
  private final Map<Identifier, Long> lastModified;
  private final ValueEncodingJackson<StoredValue> valueEncoding;
  private final CompletableFuture<Void> ready;
  private final Map<Class<? extends StoredValue>, List<String>> valueTypes;

  @Inject
  public ValueStoreImpl(
      AppContext appContext,
      Jackson jackson,
      Substitutions substitutions,
      BlobStoreFactory blobStoreFactory,
      ValueFactories valueFactories,
      VolatileRegistry volatileRegistry) {
    super(volatileRegistry);
    StoreConfiguration store = appContext.getConfiguration().getStore();
    this.blobStore = blobStoreFactory.createBlobStore(Content.VALUES);
    this.valueFactories = valueFactories;
    this.valueEncoding =
        new ValueEncodingJackson<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    this.memCache = new ConcurrentHashMap<>();
    this.lastModified = new ConcurrentHashMap<>();
    this.ready = new CompletableFuture<>();
    this.valueTypes = new ConcurrentHashMap<>();

    valueEncoding.getMapper(FORMAT.YAML).setDefaultMergeable(false);
    valueEncoding.getMapper(FORMAT.JSON).setDefaultMergeable(false);

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution(substitutions));
    valueEncoding.addDecoderMiddleware(new ValueDecoderWithBuilder<>(this::getBuilder, this));
  }

  @Override
  public String getUniqueKey() {
    return "app/store/values2";
  }

  @Override
  public int getPriority() {
    return 30;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    onVolatileStart();
    blobStore.start();

    LOGGER.debug("Loading values");

    valueFactories
        .getTypes()
        .forEach(
            valueType -> {
              ValueFactory valueFactory = valueFactories.get(valueType);
              Path typePath = Path.of(valueFactory.type());
              int count = 0;

              valueTypes.put(
                  valueFactory.valueClass(),
                  KeyValueStore.TYPE_SPLITTER.splitToList(valueFactory.type()));

              try (Stream<Path> paths =
                  blobStore.walk(
                      typePath,
                      8,
                      (path, attributes) -> attributes.isValue() && !attributes.isHidden())) {
                List<Path> files = paths.sorted().collect(Collectors.toList());

                for (Path file : files) {
                  String extension = Files.getFileExtension(file.getFileName().toString());
                  ValueEncoding.FORMAT payloadFormat = ValueEncoding.FORMAT.fromString(extension);

                  if (payloadFormat == ValueEncoding.FORMAT.UNKNOWN
                      && valueFactory.formatAliases().containsKey(extension)) {
                    payloadFormat = valueFactory.formatAliases().get(extension);
                  }

                  if (payloadFormat == ValueEncoding.FORMAT.UNKNOWN) {
                    return;
                  }

                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Loading value: {} {} {}", valueType, file, payloadFormat);
                  }

                  Path currentPath = typePath.resolve(file);
                  Path parent = Objects.requireNonNullElse(file.getParent(), Path.of(""));
                  Identifier identifier =
                      Identifier.from(
                          typePath
                              .resolve(parent)
                              .resolve(
                                  Files.getNameWithoutExtension(file.getFileName().toString())));

                  try {
                    byte[] bytes = blobStore.content(currentPath).get().readAllBytes();
                    long lm = blobStore.lastModified(currentPath);

                    StoredValue value =
                        valueEncoding.deserialize(identifier, bytes, payloadFormat, true);

                    this.memCache.put(identifier, value);
                    this.lastModified.put(identifier, lm == -1 ? Instant.now().toEpochMilli() : lm);

                    count++;
                  } catch (IOException e) {
                    LogContext.error(LOGGER, e, "Could not load value from {}", currentPath);
                  }
                }

              } catch (IOException e) {
                LogContext.error(LOGGER, e, "Could not load values with type {}", valueType);
              }

              if (count > 0) {
                LOGGER.info("Loaded {} {}", count, valueType);
              } else {
                LOGGER.debug("Loaded {} {}", count, valueType);
              }
            });

    LOGGER.debug("Loaded values");

    ready.complete(null);
    setState(State.AVAILABLE);

    return CompletableFuture.completedFuture(null);
  }

  void reload(List<Path> filter) {
    LOGGER.debug("Reloading values");

    valueFactories
        .getTypes()
        .forEach(
            valueType -> {
              ValueFactory valueFactory = valueFactories.get(valueType);
              Path typePath = Path.of(valueFactory.type());
              int count = 0;

              valueTypes.put(
                  valueFactory.valueClass(),
                  KeyValueStore.TYPE_SPLITTER.splitToList(valueFactory.type()));

              try (Stream<Path> paths =
                  blobStore.walk(
                      typePath,
                      8,
                      (path, attributes) -> attributes.isValue() && !attributes.isHidden())) {
                List<Path> files = paths.sorted().collect(Collectors.toList());

                for (Path file : files) {
                  String extension = Files.getFileExtension(file.getFileName().toString());
                  ValueEncoding.FORMAT payloadFormat = ValueEncoding.FORMAT.fromString(extension);

                  if (payloadFormat == ValueEncoding.FORMAT.UNKNOWN
                      && valueFactory.formatAliases().containsKey(extension)) {
                    payloadFormat = valueFactory.formatAliases().get(extension);
                  }

                  if (payloadFormat == ValueEncoding.FORMAT.UNKNOWN) {
                    return;
                  }

                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Loading value: {} {} {}", valueType, file, payloadFormat);
                  }

                  Path currentPath = typePath.resolve(file);
                  Path parent = Objects.requireNonNullElse(file.getParent(), Path.of(""));
                  Path identifierPath =
                      typePath
                          .resolve(parent)
                          .resolve(Files.getNameWithoutExtension(file.getFileName().toString()));
                  Identifier identifier = Identifier.from(identifierPath);

                  if (!filter.isEmpty() && filter.stream().noneMatch(identifierPath::startsWith)) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace("Skipping value, not included: {}", identifierPath);
                    }
                    continue;
                  }

                  try {
                    byte[] bytes = blobStore.content(currentPath).get().readAllBytes();
                    long lm = blobStore.lastModified(currentPath);

                    StoredValue value =
                        valueEncoding.deserialize(identifier, bytes, payloadFormat, true);

                    if (memCache.containsKey(identifier)
                        && !Objects.equals(memCache.get(identifier), value)) {
                      count++;
                    } else {
                      if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Not counting value, not changed: {}", identifierPath);
                      }
                    }

                    this.memCache.put(identifier, value);
                    this.lastModified.put(identifier, lm == -1 ? Instant.now().toEpochMilli() : lm);
                  } catch (IOException e) {
                    LogContext.error(LOGGER, e, "Could not reload value from {}", currentPath);
                  }
                }

              } catch (IOException e) {
                LogContext.error(LOGGER, e, "Could not reload values with type {}", valueType);
              }

              if (count > 0) {
                LOGGER.info("Reloaded {} {}", count, valueType);
              } else {
                LOGGER.debug("Reloaded {} {}", count, valueType);
              }
            });

    LOGGER.debug("Reloaded values");
  }

  protected ValueBuilder<StoredValue> getBuilder(Identifier identifier) {
    return valueFactories.getTypes().stream()
        .filter(type -> KeyValueStore.valueTypeMatches(identifier, type))
        .map(type -> (ValueBuilder<StoredValue>) valueFactories.get(type).builder())
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for value %s", identifier.asPath())));
  }

  @Override
  public List<Identifier> identifiers(String... path) {
    return memCache.keySet().stream()
        .filter(
            identifier ->
                identifier.path().size() >= path.length
                    && Objects.equals(identifier.path().subList(0, path.length), List.of(path)))
        .collect(Collectors.toList());
  }

  // TODO: change in KeyValueStore, might affect EntityDataStore
  @Override
  public List<String> ids(String... path) {
    return identifiers(path).stream().map(Identifier::asPath).collect(Collectors.toList());
  }

  @Override
  public boolean has(Identifier identifier) {
    return memCache.containsKey(identifier);
  }

  @Override
  public boolean has(Predicate<Identifier> matcher) {
    return memCache.keySet().stream().anyMatch(matcher);
  }

  @Override
  public StoredValue get(Identifier identifier) {
    return memCache.get(identifier);
  }

  @Override
  public CompletableFuture<StoredValue> put(Identifier identifier, StoredValue value) {
    FORMAT format = valueFactories.get(identifier).defaultFormat();
    Path path = Path.of(format.apply(identifier.asPath()));
    byte[] bytes = valueEncoding.serialize(value, format);

    try {
      blobStore.put(path, new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      return CompletableFuture.failedFuture(e);
    }

    synchronized (this) {
      memCache.put(identifier, value);

      lastModified.put(identifier, Instant.now().toEpochMilli());
    }

    return CompletableFuture.completedFuture(value);
  }

  @Override
  public CompletableFuture<Boolean> delete(Identifier identifier) {
    ValueFactory valueFactory = valueFactories.get(identifier);
    Path parent = Path.of(identifier.asPath()).getParent();
    String[] additionalExtensions = valueFactory.formatAliases().keySet().toArray(new String[0]);

    for (String extension : FORMAT.extensions(additionalExtensions)) {
      try {
        blobStore.delete(parent.resolve(identifier.id() + extension));
      } catch (IOException e) {
        // ignore
      }
    }

    StoredValue removed = null;

    synchronized (this) {
      removed = memCache.remove(identifier);

      lastModified.remove(identifier);
    }

    return CompletableFuture.completedFuture(Objects.nonNull(removed));
  }

  @Override
  public long lastModified(Identifier identifier) {
    return lastModified.getOrDefault(identifier, Instant.now().toEpochMilli());
  }

  @Override
  public CompletableFuture<Void> onReady() {
    return ready;
  }

  @Override
  public BlobStore asBlobStore() {
    return blobStore;
  }

  @Override
  public <U extends StoredValue> KeyValueStore<U> forTypeWritable(Class<U> type) {
    return new ValueStoreDecorator<StoredValue, U>() {

      @Override
      public KeyValueStore<StoredValue> getDecorated() {
        return ValueStoreImpl.this;
      }

      @Override
      public List<String> getValueType() {
        return valueTypes.get(type);
      }
    };
  }

  @Override
  public <U extends StoredValue> Values<U> forType(Class<U> type) {
    return forTypeWritable(type);
  }
}
