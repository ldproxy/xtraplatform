/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Store;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.StoreFilters;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Mode;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.base.domain.StoreSourceFs;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StoreImpl implements Store, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreImpl.class);

  private final Path dataDirectory;
  private final StoreConfiguration storeConfiguration;
  private final List<StoreSource> sources;

  @Inject
  StoreImpl(AppContext appContext) {
    this(appContext.getDataDir(), appContext.getConfiguration().getStore());
  }

  public StoreImpl(Path dataDirectory, StoreConfiguration storeConfiguration) {
    this.dataDirectory = dataDirectory;
    this.storeConfiguration = storeConfiguration;
    this.sources =
        storeConfiguration.getSources(dataDirectory).stream()
            .filter(source -> source.getContent() != Content.NONE)
            .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public int getPriority() {
    return 10;
  }

  @Override
  public void onStart() {
    LOGGER.info(
        "Loading store ({}{}{})",
        storeConfiguration.isReadOnly() ? "read-only" : "writable",
        storeConfiguration.isWatch() ? ", watching for changes" : "",
        storeConfiguration.isFiltered()
            ? String.format(", filtered by %s", storeConfiguration.getFilter().get().getAsLabel())
            : "");

    sources.forEach(
        s -> {
          String src =
              s.getType() == Type.FS
                  ? ((StoreSourceFs) s).getAbsolutePath(dataDirectory).toString()
                  : s.getSrc();
          String mode = storeConfiguration.isReadOnly() ? "" : String.format(" [%s]", s.getMode());
          String subType =
              s.getContent() == Content.RESOURCES && s.getPrefix().isPresent()
                  ? String.format(" [%s]", s.getPrefix().get())
                  : "";

          LOGGER.info("  {} [{}]{}{}", s.getLabelSpaces(), s.getContent(), subType, mode);
        });
  }

  @Override
  public List<StoreSource> get() {
    return sources;
  }

  @Override
  public List<StoreSource> get(Type type) {
    return sources.stream()
        .filter(source -> source.getType() == type)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<StoreSource> get(Content content) {
    return sources.stream()
        .filter(
            source ->
                source.getContent() == content
                    || source.getContent() == Content.ALL
                    || (content.isEvent() && source.getContent() == Content.ENTITIES))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public <U> List<U> get(Type type, Function<StoreSource, U> map) {
    return sources.stream()
        .filter(source -> source.getType() == type)
        .map(map)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public boolean has(Type type) {
    return sources.stream().anyMatch(source -> source.getType() == type);
  }

  @Override
  public Optional<StoreSource> getWritable(Type type) {
    return Lists.reverse(sources).stream()
        .filter(source -> source.getType() == type && source.getMode() == Mode.RW)
        .findFirst();
  }

  @Override
  public <U> Optional<U> getWritable(Type type, Function<StoreSource, U> map) {
    return Lists.reverse(sources).stream()
        .filter(source -> source.getType() == type && source.getMode() == Mode.RW)
        .map(map)
        .findFirst();
  }

  @Override
  public boolean isWritable() {
    return storeConfiguration.isReadWrite();
  }

  @Override
  public boolean isWatchable() {
    return storeConfiguration.isWatch();
  }

  @Override
  public Optional<StoreFilters> getFilter() {
    return storeConfiguration.getFilter();
  }
}
