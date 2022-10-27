/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@JsonDeserialize(builder = ImmutableServiceDataCommon.Builder.class)
public interface ServiceData extends EntityData, AutoEntity {

  @Override
  default Optional<String> getEntitySubType() {
    return Optional.of(getServiceType());
  }

  String getServiceType();

  /**
   * @return label for service
   */
  @Value.Default
  default String getLabel() {
    return getId();
  }

  Optional<String> getDescription();

  @JsonProperty("enabled")
  @JsonAlias("shouldStart")
  @Value.Default
  default boolean getEnabled() {
    return true;
  }

  List<Notification> getNotifications();

  @DocIgnore
  @JsonProperty("secured")
  @Value.Default
  default boolean getSecured() {
    return true;
  }

  Optional<Integer> getApiVersion();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Override
  Optional<Boolean> getAuto();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Override
  Optional<Boolean> getAutoPersist();

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
