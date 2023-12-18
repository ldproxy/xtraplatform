/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueBuilder;

public interface EntityDataBuilder<T extends EntityData> extends ValueBuilder<T> {

  @Override
  T build();

  @Override
  EntityDataBuilder<T> from(StoredValue value);

  EntityDataBuilder<T> from(EntityData value);

  // TODO: is there a better solution?
  default EntityDataBuilder<T> fillRequiredFieldsWithPlaceholders() {
    return this;
  }
}
