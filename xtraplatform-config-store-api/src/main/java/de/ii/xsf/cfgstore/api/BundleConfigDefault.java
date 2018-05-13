/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public abstract class BundleConfigDefault implements BundleConfig /*implements Resource*/ {

    protected static final LocalizedLogger LOGGER = XSFLogger.getLogger(BundleConfigDefault.class);

    private String configId;
    private String bundleId;
    private BundleConfigStore store;
    private ObjectMapper jsonMapper;
    private ConfigurationListenerRegistry listeners;
    private String category;
    protected Map<String, String> properties;

    public void init(String bundleId, String configId, BundleConfigStore store, ConfigurationListenerRegistry listeners, String category, Map<String, Map<String, String>> properties) throws IOException {
        this.bundleId = bundleId;
        this.configId = configId;
        this.store = store;
        this.jsonMapper = createMapper();
        this.listeners = listeners;
        this.category = category;

        LOGGER.getLogger().debug("BUNDLECONFIG BIND: {} {}", bundleId, configId);

        if (!getStore().hasResource(configId)) {

            LOGGER.getLogger().debug("BUNDLECONFIG ADD");

            // TODO: lower case, defaults ignored
            this.properties = properties.values().stream()
                      .filter(property -> property.containsKey("name") && property.containsKey("defaultvalue"))
                      .collect(ImmutableMap.toImmutableMap(property -> property.get("name"), property -> property.get("defaultvalue")));

            //this.properties = ImmutableMap.of("externalUrl", "bla");

            JsonBundleConfig json = serialize();

            getStore().addResource(json);
        } else {

            JsonBundleConfig json = getStore().getResource(configId);

            deserialize(json);

            LOGGER.getLogger().debug("BUNDLECONFIG GET {}", json.getProperties());
        }

        store.addConfigPropertyDescriptors(category, bundleId, configId, properties);
    }

    @Override
    public void save() throws IOException {
        LOGGER.getLogger().debug("BUNDLECONFIG SAVE");
        
        JsonBundleConfig json = serialize();

        getStore().updateResource(json);
        
        listeners.update(this);
    }

    private ResourceStore<JsonBundleConfig> getStore() {
        return store.withChild(bundleId);
    }

    private JsonBundleConfig serialize() {
        return new JsonBundleConfig(configId, properties);
    }

    private void deserialize(JsonBundleConfig json) throws IOException {
        //jsonMapper.readerForUpdating(this).readValue(json.getCfg());
        this.properties = json.getProperties();
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
