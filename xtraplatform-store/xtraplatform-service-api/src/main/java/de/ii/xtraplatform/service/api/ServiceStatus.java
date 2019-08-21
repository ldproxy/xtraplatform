/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface ServiceStatus extends ServiceData {

    enum STATUS {STARTED,STOPPED}

    STATUS getStatus();

    @Value.Default
    default boolean getHasBackgroundTask() {
        return false;
    }

    @Value.Default
    default int getProgress() {
        return 0;
    }

    @Nullable
    String getMessage();
}
