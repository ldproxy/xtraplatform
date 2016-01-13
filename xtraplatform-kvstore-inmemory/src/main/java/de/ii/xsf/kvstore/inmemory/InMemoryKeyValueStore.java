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
package de.ii.xsf.kvstore.inmemory;

import com.google.common.collect.Lists;
import de.ii.xsf.configstore.api.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 *
 * @author zahnen
 */
public class InMemoryKeyValueStore implements KeyValueStore {

    protected final String rootPath;
    protected final Map<String, KeyValueStore> childStores;
    protected final Map<String, String> resources;

    public InMemoryKeyValueStore(String rootPath) {
        this.rootPath = rootPath;
        this.childStores = new HashMap<>();
        this.resources = new HashMap<>();
    }

    @Override
    public WriteTransaction<String> openWriteTransaction(String id) {
        return new WriteInMemoryTransaction(resources, id);
    }

    @Override
    public Transaction openDeleteTransaction(String id) {
        return new DeleteInMemoryTransaction(resources, id);
    }

    @Override
    public Reader getValueReader(String id) throws KeyNotFoundException, IOException {
        if (!containsKey(id)) {
            throw new KeyNotFoundException();
        }
        return new StringReader(resources.get(id));
    }

    @Override
    public boolean containsKey(String id) {
        return resources.containsKey(id);
    }

    @Override
    public List<String> getKeys() {

        return Lists.newArrayList(resources.keySet());
    }

    @Override
    public KeyValueStore getChildStore(String... path) {
        if (path.length == 0) {
            return this;
        }
        
        if (!childStores.containsKey(path[0])) {
            childStores.put(path[0], new InMemoryKeyValueStore(path[0]));
        }
        
        if (path.length == 1) {
            return childStores.get(path[0]);
        }
        
        return childStores.get(path[0]).getChildStore(Arrays.copyOfRange(path, 1, path.length));
    }

    @Override
    public List<String> getChildStoreIds() {
        List<String> ids = new ArrayList<>();
        
        // only return non-empty child stores
        // this actually differs from the file implementation for now
        for (String id: childStores.keySet()) {
            if (!childStores.get(id).getChildStoreIds().isEmpty() || !childStores.get(id).getKeys().isEmpty()) {
                ids.add(id);
            }
        }
        
        return ids;
    }
}
