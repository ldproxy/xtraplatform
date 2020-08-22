/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;


import de.ii.xtraplatform.store.legacy.WriteTransaction;

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

        /*if (operation == UPDATE_OVERRIDE) {
            if (cache.isFullCache() && keyExists) {
            // TODO: does this really create a copy?
            // otherwise rollback will not work
                T merged = deepUpdater.applyUpdate(cache.get(key), serializer.serializeUpdate(value));
            cache.put(key, merged);
            }
        } else {*/
            cache.put(key, value);
        //}
    }

    @Override
    public void execute() throws IOException {

    }

}
