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
package de.ii.xsf.configstore.file;

import de.ii.xsf.configstore.api.KeyValueStore;
import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;
import java.io.File;
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

    private static final String ROOT_DIR_NAME = "config-store";

    public RootFileConfigStore(@Context BundleContext bc) {
        super(new File(new File(bc.getProperty(DATA_DIR_KEY)), ROOT_DIR_NAME));
        
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        if (!rootDir.isDirectory()) {
            // TODO 
            // throw exception
        }
    }
    
}
