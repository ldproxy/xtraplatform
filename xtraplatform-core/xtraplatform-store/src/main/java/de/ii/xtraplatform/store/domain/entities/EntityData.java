/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface EntityData extends de.ii.xtraplatform.store.domain.Value {

    String getId();

    @Value.Default
    default long getCreatedAt() {
        return Instant.now()
                      .toEpochMilli();
    }

    @Value.Default
    default long getLastModified() {
        return Instant.now()
                      .toEpochMilli();
    }

    @Value.Default
    default long getEntityStorageVersion() {
        return 1;
    }

    @JsonIgnore
    @Value.Derived
    default long getEntitySchemaVersion() {
        return 1;
    }

    @JsonIgnore
    Optional<String> getEntitySubType();

}
