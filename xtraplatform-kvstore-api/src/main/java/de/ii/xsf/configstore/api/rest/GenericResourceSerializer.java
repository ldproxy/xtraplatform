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
package de.ii.xsf.configstore.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xsf.core.api.Resource;
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
    public String serializeAdd(T resource) throws IOException {
        return jsonMapper.writer().writeValueAsString(resource);
    }

    @Override
    public String serializeUpdate(T resource) throws IOException {
        return jsonMapper.writer().writeValueAsString(resource);
    }
}
