/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;
import de.ii.xtraplatform.entity.api.EntityData;

/**
 * @author zahnen
 */
public abstract class AbstractService extends AbstractPersistentEntity<ImmutableServiceData> {

    @Override
    protected boolean shouldRegister() {
        return getData() != null && getData().getShouldStart();
    }

    @Override
    protected ImmutableServiceData dataToImmutable(EntityData data) {
        return ImmutableServiceData.copyOf((ServiceData) data);
    }
}
