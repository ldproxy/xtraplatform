package de.ii.xsf.kvstore.inmemory;

import de.ii.xsf.configstore.api.KeyValueStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class RootInMemoryKeyValueStore extends InMemoryKeyValueStore implements KeyValueStore {

    private static final String ROOT_DIR_NAME = "config-store";

    public RootInMemoryKeyValueStore() {
        super(ROOT_DIR_NAME);
    }

}
