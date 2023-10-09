/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.StoreSource.Content;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface Store {

  List<StoreSource> get();

  List<StoreSource> get(String type);

  List<StoreSource> get(Content content);

  <U> List<U> get(String type, Function<StoreSource, U> map);

  boolean has(String type);

  Optional<StoreSource> getWritable(String type);

  <U> Optional<U> getWritable(String type, Function<StoreSource, U> map);

  boolean isWritable();

  boolean isWatchable();

  Optional<StoreFilters> getFilter();
}
