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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfigSerializer extends GenericResourceSerializer<JsonBundleConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonBundleConfigSerializer.class);

    private ObjectMapper jsonMerge;
    
    public JsonBundleConfigSerializer(ObjectMapper jsonMapper) {
        super(jsonMapper);
        this.jsonMerge = jsonMapper.copy().setDefaultMergeable(true);
    }

    @Override
    public JsonBundleConfig deserialize(JsonBundleConfig resource, Reader reader) throws IOException {
        final Map<String,String> values = jsonMapper.readValue(reader, new TypeReference<LinkedHashMap<String,String>>(){});
        resource.setProperties(values);
        
        LOGGER.debug("LOCALBUNDLECONFIGSTORE deserialize: {}", resource.getResourceId());
        
        return resource;
    }

    @Override
    public JsonBundleConfig deserialize(String id, Class<?> clazz, Reader reader) throws IOException {
        final Map<String,String> values = jsonMapper.readValue(reader, new TypeReference<LinkedHashMap<String,String>>(){});

        LOGGER.debug("LOCALBUNDLECONFIGSTORE deserialize: {}", id);

        return new JsonBundleConfig(id, values);
    }

    @Override
    public ObjectNode deserializeMerge(Reader reader) throws IOException {
        ObjectNode object = super.deserializeMerge(reader);

        return new ObjectNode(jsonMapper.getNodeFactory(), Maps.newHashMap(ImmutableMap.of("properties", object)));
    }

    @Override
    public String serializeAdd(JsonBundleConfig resource) throws IOException {
        final String values = jsonMapper.writeValueAsString(resource.getProperties());
        LOGGER.debug("LOCALBUNDLECONFIGSTORE serializeAdd: {}", values);
        
        return values;
    }

    @Override
    public String serializeUpdate(JsonBundleConfig resource) throws IOException {
        LOGGER.debug("LOCALBUNDLECONFIGSTORE serializeUpdate");
        return serializeAdd(resource);
    }

    @Override
    public String serializeMerge(JsonBundleConfig resource) throws IOException {
        final String values = jsonMapper.writeValueAsString(ImmutableMap.of("properties", resource.getProperties()));
        LOGGER.debug("LOCALBUNDLECONFIGSTORE serializeMerge: {}", values);

        return values;
    }

    @Override
    public JsonBundleConfig mergePartial(JsonBundleConfig resource, Reader reader) throws IOException {
        final Map<String,String> values = jsonMapper.readValue(reader, new TypeReference<LinkedHashMap<String,String>>(){});
        resource.getProperties().putAll(values);

        return resource;
    }
}
