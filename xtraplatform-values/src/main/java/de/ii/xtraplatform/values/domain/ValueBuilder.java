/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

public interface ValueBuilder<T extends StoredValue> {
  T build();

  ValueBuilder<T> from(StoredValue value);

  ValueBuilder<T> preHash(String hashCode);
}
