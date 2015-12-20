package de.ii.xsf.configstore.file;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by zahnen on 21.11.15.
 */
public class DeleteFileTransaction extends AbstractFileTransaction {

    public DeleteFileTransaction(File file) {
        super(file);
    }

    @Override
    public void execute() throws IOException {
        backup();
        file.delete();
    }
}
