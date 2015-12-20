package de.ii.xsf.dropwizard.api;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author zahnen
 */
public interface Jackson {
    public ObjectMapper getDefaultObjectMapper();
}
