/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import de.ii.xtraplatform.store.domain.entities.PersistentEntity;

/**
 * @author zahnen
 */
public interface Service extends PersistentEntity {

  String TYPE = "services";

  String SERVICE_TYPE_KEY = "serviceType";

  @Override
  default String getType() {
    return TYPE;
  }

  default String getServiceType() {
    return getData().getServiceType();
  }

  @Override
  ServiceData getData();
}
