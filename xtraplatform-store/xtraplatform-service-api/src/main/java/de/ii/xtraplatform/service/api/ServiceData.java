/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.entities.domain.AutoEntity;
import de.ii.xtraplatform.entities.domain.EntityData;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface ServiceData extends EntityData, AutoEntity {

    String getServiceType();

    @Value.Default
    default String getLabel() {
        return getId();
    }

    Optional<String> getDescription();

    @Value.Default
    default boolean getShouldStart() {
        return false;
    }

    List<Notification> getNotifications();

    @Value.Default
    default boolean getSecured() {
        return false;
    }

    Optional<Integer> getApiVersion();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Override
    Optional<Boolean> getAuto();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Override
    Optional<Boolean> getAutoPersist();

    @JsonIgnore
    default boolean isLoading() {
        return false;
    }

    @JsonIgnore
    default boolean hasError() {
        return false;
    }
}
