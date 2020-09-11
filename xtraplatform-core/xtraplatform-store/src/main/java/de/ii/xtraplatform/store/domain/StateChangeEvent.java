/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(get = "*")
public interface StateChangeEvent extends Event {
  enum STATE {
    REPLAYING,
    LISTENING
  }

  STATE state();

  String type();
}
