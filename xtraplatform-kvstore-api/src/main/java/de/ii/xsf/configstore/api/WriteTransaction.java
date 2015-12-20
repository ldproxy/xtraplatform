package de.ii.xsf.configstore.api;

import java.io.IOException;

/**
 * Created by zahnen on 21.11.15.
 */
public interface WriteTransaction<T> extends Transaction {
    void write(T value) throws IOException;
}
