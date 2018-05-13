/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.ii.xsf.configstore.api.rest.GenericResourceSerializer;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfigSerializer extends GenericResourceSerializer<JsonBundleConfig> {
    
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(JsonBundleConfigSerializer.class);
    
    public JsonBundleConfigSerializer(ObjectMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public JsonBundleConfig deserialize(JsonBundleConfig resource, Reader reader) throws IOException {
        final Map<String,String> values = jsonMapper.readValue(reader, new TypeReference<LinkedHashMap<String,String>>(){});
        resource.setProperties(values);
        
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE deserialize: {}", resource.getResourceId());
        
        return resource;
    }

    @Override
    public ObjectNode deserializeMerge(Reader reader) throws IOException {
        ObjectNode object = super.deserializeMerge(reader);

        return new ObjectNode(jsonMapper.getNodeFactory(), Maps.newHashMap(ImmutableMap.of("properties", object)));
    }

    @Override
    public String serializeAdd(JsonBundleConfig resource) throws IOException {
        final String values = jsonMapper.writeValueAsString(resource.getProperties());
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE serializeAdd: {}", values);
        
        return values;
    }

    @Override
    public String serializeUpdate(JsonBundleConfig resource) throws IOException {
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE serializeUpdate");
        return serializeAdd(resource);
    }

    @Override
    public String serializeMerge(JsonBundleConfig resource) throws IOException {
        final String values = jsonMapper.writeValueAsString(ImmutableMap.of("properties", resource.getProperties()));
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE serializeMerge: {}", values);

        return values;
    }
}
