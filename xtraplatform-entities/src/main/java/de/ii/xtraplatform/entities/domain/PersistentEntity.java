/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

/**
 * @author zahnen
 */
public interface PersistentEntity {

  default String getId() {
    return getData() != null ? getData().getId() : null;
  }

  String getType();

  EntityData getData();

  ChangingData getChangingData();
}
