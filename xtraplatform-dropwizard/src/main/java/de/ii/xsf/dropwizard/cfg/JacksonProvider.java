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
