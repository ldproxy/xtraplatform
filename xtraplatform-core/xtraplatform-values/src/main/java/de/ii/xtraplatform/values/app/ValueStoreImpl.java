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
import de.ii.xtraplatform.base.domain.StoreSource.Content;
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
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = {ValueStore.class, Values.class, AppLifeCycle.class})
public class ValueStoreImpl implements ValueStore, ValueCache<StoredValue>, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueStoreImpl.class);

  private final BlobStore blobStore;
  private final ValueFactories valueFactories;
  private final Map<Identifier, StoredValue> memCache;
  private final ValueEncodingJackson<StoredValue> valueEncoding;
  private final CompletableFuture<Void> ready;

  @Inject
  public ValueStoreImpl(
      AppContext appContext,
      Jackson jackson,
      BlobStoreFactory blobStoreFactory,
      ValueFactories valueFactories) {
    this.blobStore = blobStoreFactory.createBlobStore(Content.VALUES);
    this.valueFactories = valueFactories;
    this.valueEncoding =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().getStore().isFailOnUnknownProperties());
    this.memCache = new ConcurrentHashMap<>();
    this.ready = new CompletableFuture<>();

    valueEncoding.getMapper(FORMAT.YAML).setDefaultMergeable(false);
    valueEncoding.getMapper(FORMAT.JSON).setDefaultMergeable(false);

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(new ValueDecoderWithBuilder<>(this::getBuilder, this));
  }

  @Override
  public int getPriority() {
    return 55;
  }

  @Override
  public void onStart() {
    blobStore.start();

    LOGGER.debug("Loading values");

    valueFactories
        .getTypes()
        .forEach(
            valueType -> {
              ValueFactory valueFactory = valueFactories.get(valueType);
              Path typePath = Path.of(valueFactory.type());
              int count = 0;

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
                  Identifier identifier =
                      Identifier.from(
                          typePath
                              .resolve(Objects.requireNonNullElse(file.getParent(), Path.of("")))
                              .resolve(
                                  Files.getNameWithoutExtension(file.getFileName().toString())));

                  try {
                    byte[] bytes = blobStore.content(currentPath).get().readAllBytes();

                    StoredValue value =
                        valueEncoding.deserialize(identifier, bytes, payloadFormat, true);

                    this.memCache.put(identifier, value);

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
    memCache.put(identifier, value);

    return CompletableFuture.completedFuture(value);
  }

  @Override
  public CompletableFuture<Boolean> delete(Identifier identifier) {
    StoredValue removed = memCache.remove(identifier);

    return CompletableFuture.completedFuture(Objects.nonNull(removed));
  }

  // TODO
  @Override
  public long lastModified(Identifier identifier) {
    return ValueStore.super.lastModified(identifier);
  }

  @Override
  public CompletableFuture<Void> onReady() {
    return ready;
  }

  @Override
  public <U extends StoredValue> KeyValueStore<U> forType(Class<U> type) {
    final List<String> valueType =
        KeyValueStore.TYPE_SPLITTER.splitToList(valueFactories.get(type).type());

    return new ValueStoreDecorator<StoredValue, U>() {

      @Override
      public KeyValueStore<StoredValue> getDecorated() {
        return ValueStoreImpl.this;
      }

      @Override
      public List<String> getValueType() {
        return valueType;
      }
    };
  }
}
