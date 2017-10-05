/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
