/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zahnen
 */
public class EntityConfigurationTestImpl implements EntityConfiguration {
    private String id;
    private String type;
    private String values;

    public EntityConfigurationTestImpl() {

    }

    public EntityConfigurationTestImpl(String id, String values) {
        this();
        this.id = id;
        this.values = values;
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Override
    public void setResourceId(String id) {
        this.id = id;
    }

    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    @Override
    public String getType() {
        return type;
    }
}
