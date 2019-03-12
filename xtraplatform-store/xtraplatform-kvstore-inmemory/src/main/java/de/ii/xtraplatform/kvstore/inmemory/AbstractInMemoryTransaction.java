/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.kvstore.inmemory;

import de.ii.xtraplatform.kvstore.api.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by zahnen on 21.11.15.
 */
public abstract class AbstractInMemoryTransaction implements Transaction {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractInMemoryTransaction.class);

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
