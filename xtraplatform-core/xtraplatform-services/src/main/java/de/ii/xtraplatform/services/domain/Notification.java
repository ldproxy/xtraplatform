/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.util.Map;

/**
 *
 * @author zahnen
 */
@Value.Immutable
public abstract class Notification {

    public enum LEVEL {
        ERROR,
        WARNING,
        INFO
    }

    public static final String DEFAULT_LANGUAGE = "en";

    public abstract LEVEL getLevel();

    @Value.Default
    public Map<String, String> getMessages() {
        return ImmutableMap.of(DEFAULT_LANGUAGE, "");
    };

    @Value.Derived
    public String getMessage() {
        return getMessages().getOrDefault(DEFAULT_LANGUAGE, "");
    }
}
