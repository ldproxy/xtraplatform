/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
public interface ResourceSerializer<T extends Resource> {

    T deserialize(T resource, Reader reader) throws IOException;

    T deserialize(String id, Class<?> clazz, Reader reader) throws IOException;


    ObjectNode deserializeMerge(Reader reader) throws IOException;

    String serializeAdd(T resource) throws IOException;

    String serializeUpdate(T resource) throws IOException;

    String serializeMerge(T resource) throws IOException;

    default Optional<T> deserializePartial(Class<?> clazz, Reader reader) throws IOException {
        return Optional.empty();
    }

    default T mergePartial(T resource, String partial) throws IOException {
        return null;
    }

    default T mergePartial(T resource, Reader reader) throws IOException {
        return null;
    }
}
