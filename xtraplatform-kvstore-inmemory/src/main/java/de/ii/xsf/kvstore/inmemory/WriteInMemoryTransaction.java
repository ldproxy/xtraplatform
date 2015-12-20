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
