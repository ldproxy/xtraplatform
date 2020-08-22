/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.file;

import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;
import java.io.File;

import de.ii.xtraplatform.store.legacy.KeyValueStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class RootFileConfigStore extends FileConfigStore implements KeyValueStore {

    private static final String ROOT_DIR_NAME = "store";

    public RootFileConfigStore(@Context BundleContext bc) {
        super(new File(new File(bc.getProperty(DATA_DIR_KEY)), ROOT_DIR_NAME));
        
        if (!rootDir.exists()) {
            //rootDir.mkdirs();
        }
        if (!rootDir.isDirectory()) {
            // TODO 
            // throw exception
        }
    }
    
}
