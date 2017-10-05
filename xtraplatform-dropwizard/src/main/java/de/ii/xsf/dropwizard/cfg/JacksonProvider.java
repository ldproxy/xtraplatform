/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public ObjectMapper getDefaultObjectMapper() {
        return jsonMapper;
    }
    
}
