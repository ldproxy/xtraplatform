/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.kvstore.inmemory;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class DeleteInMemoryTransaction extends AbstractInMemoryTransaction {

    public DeleteInMemoryTransaction(Map<String, String> resources, String key) {
        super(resources, key);
    }

    @Override
    public void execute() throws IOException {
        resources.remove(key);
    }

}
