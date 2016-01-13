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

import de.ii.xsf.configstore.api.Transaction;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.util.Map;

/**
 * Created by zahnen on 21.11.15.
 */
public abstract class AbstractInMemoryTransaction implements Transaction {
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractInMemoryTransaction.class);
    protected final Map<String, String> resources;
    protected final String key;
    private final String backup;

    public AbstractInMemoryTransaction(Map<String, String> resources, String key) {
        this.resources = resources;
        this.key = key;
        this.backup = resources.get(key);
    }

    @Override
    public void commit() {

    }

    @Override
    public void rollback() {
        if (backup != null) {
            resources.put(key, backup);
        } else {
            resources.remove(key);
        }
    }

    @Override
    public void close() {

    }
}
