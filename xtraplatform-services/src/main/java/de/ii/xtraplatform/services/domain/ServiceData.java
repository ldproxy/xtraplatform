/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entities.domain.AutoEntity;
import de.ii.xtraplatform.entities.domain.EntityData;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonDeserialize(builder = ImmutableServiceDataCommon.Builder.class)
public interface ServiceData extends EntityData, AutoEntity {

  @Override
  default Optional<String> getEntitySubType() {
    return Optional.of(getServiceType());
  }

  String getServiceType();

  /**
   * @langEn Human readable label.
   * @langDe Menschenlesbare Bezeichnung.
   * @default {id}
   */
  @Value.Default
  default String getLabel() {
    return getId();
  }

  /**
   * @langEn Human readable description.
   * @langDe Menschenlesbare Beschreibung.
   * @default ""
   */
  Optional<String> getDescription();

  /**
   * @langEn Option to disable the service, which means its REST API will not be available and
   *     background tasks will not be running.
   * @langDe Option um den Dienst zu deaktivieren, was bedeutet, dass seine REST API nicht
   *     erreichbar ist und Hintergrundprozesse nicht laufen.
   * @default true
   */
  @Value.Default
  default boolean getEnabled() {
    return true;
  }

  /**
   * @langEn Adds a version to the URL path, instead of `/{id}` it will be `/{id}/v{apiVersion}`.
   * @langDe Fügt dem URL-Pfad eine Version hinzu , anstatt von `/{id}` ist dieser dann
   *     `/{id}/v{apiVersion}`.
   * @default null
   */
  Optional<Integer> getApiVersion();

  /**
   * @langEn Automatically generate missing configuration options on startup if possible.
   * @langDe Automatische fehlende Konfigurationsoptionen beim Start generieren.
   */
  @JsonProperty(value = "auto", access = JsonProperty.Access.WRITE_ONLY)
  @Override
  Optional<Boolean> getAuto();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<String> getSubPath() {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
    builder.add(getId());
    if (getApiVersion().isPresent()) {
      builder.add("v" + getApiVersion().get());
    }
    return builder.build();
  }
}
