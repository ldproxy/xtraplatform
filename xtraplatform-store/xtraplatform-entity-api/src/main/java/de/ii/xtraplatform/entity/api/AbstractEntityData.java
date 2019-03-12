/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.api.Resource;

/**
 * @author zahnen
 */
// TODO: move
public abstract class AbstractEntityData implements EntityData, /*TODO remove*/Resource {

    public abstract String getId();

    public abstract long getCreatedAt();

    public abstract long getLastModified();

    @JsonIgnore
    @Override
    public String getResourceId() {
        return getId();
    }

    @Override
    public void setResourceId(String id) {

    }
}
