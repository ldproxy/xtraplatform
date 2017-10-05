/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.configstore.api;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 *
 * @author zahnen
 */
public interface KeyValueStore extends TransactionSupport<String> {

    /**
     *
     * @param path the path of the child KeyValueStore. If the Store does not exist it will be created
     * @return
     */
    public KeyValueStore getChildStore(String... path);

    /**
     * 
     * 
     * @return the ids of the direct child stores
     */
    public List<String> getChildStoreIds();

    /**
     *
     * @return
     */
    public List<String> getKeys();
    
    /**
     * 
     *
     * @param key the key
     * @return 
     */
    public boolean containsKey(String key);

    /**
     *
     * @param key the key
     * @return a reader for the value
     * @throws KeyNotFoundException
     * @throws IOException
     */
    public Reader getValueReader(String key) throws KeyNotFoundException, IOException;

    
}
