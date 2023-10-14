/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.values.domain.Builder;
import de.ii.xtraplatform.values.domain.Value;

public interface EntityDataBuilder<T extends EntityData> extends Builder<T> {

  @Override
  T build();

  @Override
  EntityDataBuilder<T> from(Value value);

  EntityDataBuilder<T> from(EntityData value);

  // TODO: is there a better solution?
  default EntityDataBuilder<T> fillRequiredFieldsWithPlaceholders() {
    return this;
  }
}
