/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ii.xsf.configstore.api.rest.GenericResourceSerializer;
import de.ii.xsf.logging.XSFLogger;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfigSerializer extends GenericResourceSerializer<JsonBundleConfig> {
    
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(JsonBundleConfigSerializer.class);
    
    public JsonBundleConfigSerializer(ObjectMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public JsonBundleConfig deserialize(JsonBundleConfig resource, Reader reader) throws IOException {
        ObjectNode node = (ObjectNode)jsonMapper.readTree(reader);
        
        // TODO: this might not survive a refactoring
        //resource.setResourceId(node.get("resourceId").textValue());
        resource.setCfg(node);
        
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE deserialize: {}", resource.getResourceId());
        
        return resource;
    }

    @Override
    public String serializeAdd(JsonBundleConfig resource) throws IOException {
        StringWriter wr = new StringWriter();
        JsonGenerator gen = jsonMapper.getFactory().createGenerator(wr);
        jsonMapper.writeTree(gen, resource.getCfg());
        
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE serializeAdd: {}", wr.toString());
        
        return wr.toString();
    }

    @Override
    public String serializeUpdate(JsonBundleConfig resource) throws IOException {
        LOGGER.getLogger().debug("LOCALBUNDLECONFIGSTORE serializeUpdate");
        return serializeAdd(resource);
    }
    
    
}
