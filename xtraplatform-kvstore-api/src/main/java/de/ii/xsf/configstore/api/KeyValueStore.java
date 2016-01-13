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
