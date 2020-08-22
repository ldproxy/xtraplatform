/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author zahnen
 */
public class GenericResourceSerializer<T extends Resource> implements ResourceSerializer<T> {
    
    protected final ObjectMapper jsonMapper;

    public GenericResourceSerializer(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
    
    @Override
    public T deserialize(T resource, Reader reader) throws IOException {
        jsonMapper.readerForUpdating(resource).readValue(reader);
        return resource;
    }

    @Override
    public T deserialize(String id, Class<?> clazz, Reader reader) throws IOException {
        return (T) jsonMapper.readValue(reader, clazz);
    }

    @Override
    public ObjectNode deserializeMerge(Reader reader) throws IOException {
        return (ObjectNode) jsonMapper.readTree(reader);
    }

    @Override
    public String serializeAdd(T resource) throws IOException {
        return jsonMapper.writer().writeValueAsString(resource);
    }

    @Override
    public String serializeUpdate(T resource) throws IOException {
        return jsonMapper.writer().writeValueAsString(resource);
    }

    @Override
    public String serializeMerge(T resource) throws IOException {
        return jsonMapper.writer().writeValueAsString(resource);
    }
}
