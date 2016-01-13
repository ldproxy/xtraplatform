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
package de.ii.xsf.core.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;

/**
 *
 * @author fischer
 */
public class DeepUpdater<T> {

    protected final ObjectMapper jsonMapper;
                                                
    public DeepUpdater(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void applyUpdate(T orig, File json) throws IOException {
        String update = Files.toString(json, Charset.forName("UTF-8"));
        applyUpdate(orig, update);
    }

    public void applyUpdate(T orig, String json) throws IOException {
        ObjectNode update = (ObjectNode) jsonMapper.readTree(json);
        applyUpdate(orig, update);
    }

    public void applyUpdate(T orig, Reader jsonReader) throws IOException {
        ObjectNode update = (ObjectNode) jsonMapper.readTree(jsonReader);
        applyUpdate(orig, update);
    }
    
    public T applyUpdate(final T orig, final T obj) throws IOException {
        T original = (T) jsonMapper.convertValue(orig, orig.getClass());
        ObjectNode update = (ObjectNode) jsonMapper.valueToTree(obj);
        
        applyUpdate(original, update);
        
        return original;
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
                    Object o2 = PropertyUtils.getProperty(original, fieldEntry.getKey());
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
                    Object o2 = PropertyUtils.getProperty(original, fieldEntry.getKey());
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
