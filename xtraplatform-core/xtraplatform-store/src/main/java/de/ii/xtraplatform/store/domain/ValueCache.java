/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import java.util.function.Predicate;

public interface ValueCache<T> {
  boolean isInCache(Identifier identifier);

  boolean isInCache(Predicate<Identifier> keyMatcher);

  T getFromCache(Identifier identifier);
}
