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
