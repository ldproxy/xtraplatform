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
