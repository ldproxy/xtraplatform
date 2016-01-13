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
