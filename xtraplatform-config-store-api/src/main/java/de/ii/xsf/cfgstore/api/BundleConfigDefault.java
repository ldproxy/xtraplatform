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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;

/**
 *
 * @author zahnen
 */
public abstract class BundleConfigDefault implements BundleConfig /*implements Resource*/ {

    protected static final LocalizedLogger LOGGER = XSFLogger.getLogger(BundleConfigDefault.class);

    private String configId;
    private String bundleId;
    private ResourceStore<JsonBundleConfig> store;
    private ObjectMapper jsonMapper;
    private ConfigurationListenerRegistry listeners;

    public void init(String bundleId, String configId, ResourceStore<JsonBundleConfig> store, ConfigurationListenerRegistry listeners) throws IOException {
        this.bundleId = bundleId;
        this.configId = configId;
        this.store = store;
        this.jsonMapper = createMapper();
        this.listeners = listeners;

        LOGGER.getLogger().debug("BUNDLECONFIG BIND: {} {}", bundleId, configId);

        if (!store.withChild(bundleId).hasResource(configId)) {

            LOGGER.getLogger().debug("BUNDLECONFIG ADD");

            JsonBundleConfig json = serialize();

            store.withChild(bundleId).addResource(json);
        } else {

            JsonBundleConfig json = store.withChild(bundleId).getResource(configId);

            deserialize(json);

            LOGGER.getLogger().debug("BUNDLECONFIG GET {}", json.getCfg());
        }
    }

    @Override
    public void save() throws IOException {
        LOGGER.getLogger().debug("BUNDLECONFIG SAVE");
        
        JsonBundleConfig json = serialize();

        store.withChild(bundleId).updateResource(json);
        
        listeners.update(this);
    }

    private JsonBundleConfig serialize() {
        return new JsonBundleConfig(configId, (ObjectNode) jsonMapper.valueToTree(this));
    }

    private void deserialize(JsonBundleConfig json) throws IOException {
        jsonMapper.readerForUpdating(this).readValue(json.getCfg());
    }

    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.AUTO_DETECT_CREATORS);
        mapper.disable(MapperFeature.AUTO_DETECT_FIELDS);
        mapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
        mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
        //jsonMapper.disable(MapperFeature.AUTO_DETECT_SETTERS);
        //jsonMapper.disable(MapperFeature.USE_ANNOTATIONS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mapper;
    }
}
