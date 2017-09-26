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
package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.configstore.api.Transaction;
import de.ii.xsf.core.api.Resource;
import de.ii.xsf.core.util.json.DeepUpdater;
import java.io.IOException;

/**
 *
 * @author fischer
 */
public abstract class AbstractCacheTransaction<T extends Resource> implements Transaction {

    protected final ResourceCache<T> cache;
    protected final String key;
    private final T backup;
    protected final boolean keyExists;
    protected ResourceTransaction.OPERATION operation;
    protected DeepUpdater<T> deepUpdater;

    public AbstractCacheTransaction(ResourceCache<T> cache, String key) {
        this.cache = cache;
        this.key = key;
        this.backup = cache.get(key);
        this.keyExists = cache.hasResource(key);
    }
    
    @Override
    public void commit() {

    }

    @Override
    public void rollback() {
        if (backup != null || keyExists) {
            cache.put(key, backup);
        } else {
            cache.remove(key);
        }
    }

    @Override
    public void close() {
        
    }
    
    public void setOperation(ResourceTransaction.OPERATION operation) {
        this.operation = operation;
    }

    public void setDeepUpdater(DeepUpdater<T> deepUpdater) {
        this.deepUpdater = deepUpdater;
    }
}
