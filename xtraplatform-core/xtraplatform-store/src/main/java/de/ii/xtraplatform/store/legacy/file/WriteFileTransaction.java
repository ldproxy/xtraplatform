/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.file;


import de.ii.xtraplatform.store.legacy.WriteTransaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
