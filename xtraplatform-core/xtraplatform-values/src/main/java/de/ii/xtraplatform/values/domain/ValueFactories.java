/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import java.util.Set;

public interface ValueFactories {

  ValueFactory get(String entityType);

  ValueFactory get(Identifier identifier);

  ValueFactory get(Class<? extends StoredValue> valueClass);

  Set<String> getTypes();
}
