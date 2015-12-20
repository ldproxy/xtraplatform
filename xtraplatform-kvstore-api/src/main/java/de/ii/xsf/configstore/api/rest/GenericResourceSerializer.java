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
