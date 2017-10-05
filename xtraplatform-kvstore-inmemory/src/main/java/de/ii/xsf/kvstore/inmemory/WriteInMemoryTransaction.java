/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.kvstore.inmemory;

import de.ii.xsf.configstore.api.WriteTransaction;

import java.io.IOException;
import java.util.Map;

/**
 * Created by zahnen on 21.11.15.
 */
public class WriteInMemoryTransaction extends AbstractInMemoryTransaction implements WriteTransaction<String> {

    public WriteInMemoryTransaction(Map<String, String> resources, String key) {
        super(resources, key);
    }
    @Override
    public void write(String value) throws IOException {
        resources.put(key, value);
    }

    @Override
    public void execute() throws IOException {

    }
}
