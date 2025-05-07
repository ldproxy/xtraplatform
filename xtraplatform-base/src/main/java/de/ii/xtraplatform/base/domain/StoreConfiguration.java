/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.docs.DocIgnore;
import io.dropwizard.util.DataSize;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableStoreConfiguration.class)
public interface StoreConfiguration {

  enum StoreMode {
    READ_WRITE,
    READ_ONLY,
    RO,
    RW,
  }

  /**
   * @langEn `RW` or `RO`. Set to `RO` if ldproxy should not be allowed to write anything.
   *     Otherwise, see `mode` for [Store sources](#store-sources).
   * @langDe `RW` oder `RO`. Kann auf `RO` gesetzt werden, falls jeglicher Schreibvorgang verboten
   *     werden soll. Ansonsten siehe `mode` für [Store sources](#store-sources).
   * @default RW
   */
  @Value.Default
  default StoreMode getMode() {
    return StoreMode.RW;
  }

  @DocIgnore
  @Value.Default
  default boolean isWatch() {
    return false;
  }

  @DocIgnore
  @Value.Default
  default boolean isFailOnUnknownProperties() {
    return false;
  }

  /**
   * @langEn YAML files for entities and values that are larger than this size will be ignored with
   *     an error.
   * @langDe YAML-Dateien für Entities und Values, die größer als dieser Wert sind, werden mit einem
   *     Fehler ignoriert.
   * @default 3MB
   * @since v4.4
   */
  @Value.Default
  default DataSize getMaxYamlFileSize() {
    return DataSize.megabytes(3);
  }

  /**
   * @langEn List of [Store sources](#store-sources). The default is the data directory. The list is
   *     appendable, which means entries from configuration files will be appended to the default.
   * @langDe Liste von [Store sources](#store-sources). Der Default ist das Data-Verzeichnis. Die
   *     Liste ist erweiterbar, d.h. Einträge aus Konfigurationsdateien werden zum Default
   *     hinzugefügt.
   * @default [{type:FS,src:.}]
   * @since v3.5
   */
  @JsonMerge
  List<StoreSource> getSources();

  @DocIgnore
  Optional<StoreFilters> getFilter();

  @JsonIgnore
  @Value.Derived
  default boolean isReadOnly() {
    return getMode() == StoreMode.RO || getMode() == StoreMode.READ_ONLY;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isReadWrite() {
    return getMode() == StoreMode.RW || getMode() == StoreMode.READ_WRITE;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isFiltered() {
    return getFilter().isPresent();
  }

  @Value.Check
  default StoreConfiguration explodeMultiParts() {
    if (getSources().stream().anyMatch(storeSource -> storeSource.getContent() == Content.MULTI)) {
      return new ImmutableStoreConfiguration.Builder()
          .from(this)
          .sources(
              getSources().stream()
                  .flatMap(
                      storeSource -> {
                        if (storeSource.getContent() == Content.MULTI) {
                          return storeSource.explode().stream();
                        }
                        return Stream.of(storeSource);
                      })
                  .collect(Collectors.toList()))
          .build();
    }

    return this;
  }
}
