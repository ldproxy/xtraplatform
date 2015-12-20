package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.configstore.api.WriteTransaction;
import de.ii.xsf.core.api.Resource;

import java.io.IOException;

/**
 * Created by zahnen on 21.11.15.
 */
public class WriteCacheTransaction<T extends Resource> extends AbstractCacheTransaction<T> implements WriteTransaction<T> {

    public WriteCacheTransaction(ResourceCache<T> cache, String key) {
        super(cache, key);
    }

    @Override
    public void write(T value) throws IOException {

        if (operation == ResourceTransaction.OPERATION.UPDATE_OVERRIDE) {
            // TODO: does this really create a copy?
            // otherwise rollback will not work
            T merged = deepUpdater.applyUpdate(cache.get(key), value);
            cache.put(key, merged);
        } else {
            cache.put(key, value);
        }
    }

    @Override
    public void execute() throws IOException {

    }

}
