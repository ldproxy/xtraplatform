/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
//@JsonSerialize(as = ImmutableFeatureProviderExample.class)
@JsonDeserialize(as = ModifiableFeatureProviderExample.class)
public abstract class FeatureProviderExample {

    @Value.Default
    public boolean getUseBasicAuth() {
        return false;
    }

    public abstract Optional<String> getBasicAuthCredentials();
}
