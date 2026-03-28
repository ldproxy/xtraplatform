/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableServiceDataCommon.Builder.class)
public interface ServiceDataCommon extends ServiceData {

  abstract class Builder implements EntityDataBuilder<ServiceData> {}
}
