package de.ii.xsf.dropwizard.cfg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.ii.xsf.dropwizard.api.Jackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 *
 * @author zahnen
 */

@Component
@Provides
@Instantiate
public class JacksonProvider implements Jackson {

    private final ObjectMapper jsonMapper;
    
    public JacksonProvider() {
        jsonMapper = new ObjectMapper();
        //jsonMapper.disable(MapperFeature.USE_ANNOTATIONS);
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public ObjectMapper getDefaultObjectMapper() {
        return jsonMapper;
    }
    
}
