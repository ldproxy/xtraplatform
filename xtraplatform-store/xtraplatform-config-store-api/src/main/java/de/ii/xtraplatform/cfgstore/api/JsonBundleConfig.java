/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cfgstore.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.api.Resource;

import java.util.Map;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfig /*extends ObjectNode*/ implements Resource {

    private String id;
    private Map<String, String> properties;

    public JsonBundleConfig(String id, Map<String, String> properties) {
        //super(JsonNodeFactory.instance);
        this.id = id;
        this.properties = properties;
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Override
    public void setResourceId(String id) {
        this.id = id;
    }

    @JsonProperty
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
