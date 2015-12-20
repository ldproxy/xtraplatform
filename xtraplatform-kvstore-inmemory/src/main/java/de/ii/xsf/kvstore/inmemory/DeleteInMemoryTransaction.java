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
