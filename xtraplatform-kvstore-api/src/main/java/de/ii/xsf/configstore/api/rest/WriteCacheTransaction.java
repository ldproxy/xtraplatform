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

import de.ii.xsf.configstore.api.WriteTransaction;
import de.ii.xsf.core.api.Resource;

import java.io.IOException;

import static de.ii.xsf.configstore.api.rest.ResourceTransaction.OPERATION.UPDATE_OVERRIDE;

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
