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

import de.ii.xsf.configstore.api.Transaction;
import de.ii.xsf.logging.XSFLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author fischer
 */
public abstract class AbstractFileTransaction implements Transaction {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractFileTransaction.class);
    private static final String BACKUP_EXTENSION = "-backup";

    protected final File file;
    private final File backup;

    public AbstractFileTransaction(File file) {
        this.file = file;
        this.backup = new File(this.file.getAbsolutePath() + BACKUP_EXTENSION);
    }

    protected void backup() throws IOException {
        // this is needed the first time a new resource is created (copy to backup)
        if (file.exists()) {
            Files.copy(file.toPath(), backup.toPath(), REPLACE_EXISTING);
        }
    }

    @Override
    public void commit() {
        if (file.exists() && fileIsEmpty(file)) {
            file.delete();
        }

        backup.delete();
    }

    @Override
    public void rollback() {

        try {
            if (backup.exists()) {
                Files.copy(backup.toPath(), file.toPath(), REPLACE_EXISTING);
                backup.delete();
            }
            if (file.exists() && fileIsEmpty(file)) {
                file.delete();
            }
        } catch (IOException ex) {
            LOGGER.getLogger().error("Rollback failed for file: {}", file.getAbsolutePath(), ex);
        }
    }

    private boolean fileIsEmpty(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.readLine() == null;
        } catch (IOException ex) {
            LOGGER.getLogger().error("File is empty failed for file: {}", file.getAbsolutePath(), ex);
        }
        return false;
    }


    @Override
    public void close() {

    }
}
