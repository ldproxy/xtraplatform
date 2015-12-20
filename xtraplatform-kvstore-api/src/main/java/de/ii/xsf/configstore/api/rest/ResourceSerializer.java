package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.core.api.Resource;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author zahnen
 */
public interface ResourceSerializer<T extends Resource> {

    T deserialize(T resource, Reader reader) throws IOException;

    String serializeAdd(T resource) throws IOException;

    String serializeUpdate(T resource) throws IOException;
}
