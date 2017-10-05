/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import de.ii.xsf.logging.XSFLogger;
import org.apache.commons.beanutils.PropertyUtils;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class DeepUpdater<T> {
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(DeepUpdater.class);

    protected final ObjectMapper jsonMapper;
                                                
    public DeepUpdater(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void applyUpdate(T orig, File json) throws IOException {
        String update = Files.toString(json, Charset.forName("UTF-8"));
        applyUpdate(orig, update);
    }

    public T applyUpdate(T orig, String json) throws IOException {
        LOGGER.getLogger().debug("APPLY UPDATE {}", json);
        ObjectNode update = (ObjectNode) jsonMapper.readTree(json);
        applyUpdate(orig, update);

        return orig;
    }

    public void applyUpdate(T orig, Reader jsonReader) throws IOException {
        ObjectNode update = (ObjectNode) jsonMapper.readTree(jsonReader);
        applyUpdate(orig, update);
    }
    
    public T applyUpdate(final T orig, final T obj) throws IOException {
        ObjectNode update = (ObjectNode) jsonMapper.valueToTree(obj);
        
        applyUpdate(orig, update);
        
        return orig;
    }

    // recursion
    protected void applyUpdate(Object original, ObjectNode updateRoot) throws IOException {
        for (Iterator<Map.Entry<String, JsonNode>> i = updateRoot.fields(); i.hasNext();) {
            Map.Entry<String, JsonNode> fieldEntry = i.next();
            JsonNode child = fieldEntry.getValue();

            if (child.isArray()) {
                try {
                    // We ignore arrays so they get instantiated fresh every time
                    // root.remove(fieldEntry.getKey());
                    Object o2 = null;
                    if (original instanceof Map)
                        o2 = ((Map)original).get(fieldEntry.getKey());
                    else
                        o2 = PropertyUtils.getProperty(original, fieldEntry.getKey());
                    if (o2 != null && !(o2 instanceof int[])) {

                        this.processFieldOfTypeArray(i, fieldEntry, o2, (T) original);

                        int j = 0;
                        for (Object o3 : (Iterable) o2) {
                            if (child.has(j)) {
                                applyUpdate(o3, (ObjectNode) child.get(j));
                            }
                            j++;
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {

                }
            } else if (child.isObject()) {
                try {
                    Object o2 = null;
                    if (original instanceof Map)
                        o2 = ((Map)original).get(fieldEntry.getKey());
                    else
                        o2 = PropertyUtils.getProperty(original, fieldEntry.getKey());
                    if (o2 != null) {
                        // Only remove the JsonNode if the object already exists
                        // Otherwise it will be instantiated when the parent gets
                        // deserialized

                        this.processFieldOfTypeObject(i, fieldEntry, o2, child);

                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {

                }
            }
        }
        jsonMapper.readerForUpdating(original).readValue(updateRoot);
    }

    protected void processFieldOfTypeObject(Iterator<Map.Entry<String, JsonNode>> i, Map.Entry<String, JsonNode> fieldEntry, Object o2, JsonNode child) throws IOException {
        i.remove();
        applyUpdate(o2, (ObjectNode) child);
    }

    /* ABLEITUNG
     protected void processFieldOfTypeObject(Iterator<Map.Entry<String, JsonNode>> i, Map.Entry<String, JsonNode> fieldEntry, Object o2, JsonNode child) throws IOException {
     if (!(fieldEntry.getKey().equals("elementMappings") && o2 instanceof Map && ((Map) o2).isEmpty())) {
     i.remove();
     execute(o2, (ObjectNode) child);
     }
     }*/
    protected void processFieldOfTypeArray(Iterator<Map.Entry<String, JsonNode>> i, Map.Entry<String, JsonNode> fieldEntry, Object o2, T original) {
        i.remove();

    }

    /* ABLEITUNG
     protected void processFieldOfTypeArray(Iterator<Map.Entry<String, JsonNode>> i, Map.Entry<String, JsonNode> fieldEntry, Object o2) {
     if (!fieldEntry.getKey().equals("fields")
     && !fieldEntry.getKey().equals("fieldsConfig")
     && !fieldEntry.getKey().equals("labelingInfo")) {
     i.remove();
     }
    
     if (service != null && fieldEntry.getKey().equals("fullLayers") && ((List) o2).isEmpty()) {
     for (int k = 0; k < service.getFullLayers().size(); k++) {
     ((List) o2).add(new WFS2GSFSLayer());
     }
     }
     }*/
}
