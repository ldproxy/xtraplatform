package de.ii.xsf.configstore.file;

import de.ii.xsf.configstore.api.WriteTransaction;

import java.io.*;

/**
 * Created by zahnen on 21.11.15.
 */
public class WriteFileTransaction extends AbstractFileTransaction implements WriteTransaction<String> {

    private Writer writer;

    public WriteFileTransaction(File file) {
        super(file);
    }

    @Override
    public void execute() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void write(String value) throws IOException {
        if (writer == null) {
            backup();

            // FileWriter uses system encoding, we don't want that
            this.writer = new OutputStreamWriter(new FileOutputStream(file), FileConfigStore.ENCODING);
        }

        writer.write(value);
    }
}
