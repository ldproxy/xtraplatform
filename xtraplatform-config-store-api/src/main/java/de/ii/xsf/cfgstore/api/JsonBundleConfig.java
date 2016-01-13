/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.cfgstore.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ii.xsf.core.api.Resource;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfig /*extends ObjectNode*/ implements Resource {

    private String id;
    private ObjectNode cfg;

    public JsonBundleConfig(String id, ObjectNode cfg) {
        //super(JsonNodeFactory.instance);
        this.id = id;
        this.cfg = cfg;
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Override
    public void setResourceId(String id) {
        this.id = id;
    }

    public ObjectNode getCfg() {
        return cfg;
    }

    public void setCfg(ObjectNode cfg) {
        this.cfg = cfg;
    }
    
    
}
