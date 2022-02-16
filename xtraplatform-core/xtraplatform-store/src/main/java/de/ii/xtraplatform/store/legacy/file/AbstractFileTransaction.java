/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.file;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.store.legacy.Transaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author fischer */
public abstract class AbstractFileTransaction implements Transaction {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileTransaction.class);
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
      LogContext.error(LOGGER, ex, "Rollback failed for file", file.getAbsolutePath());
    }
  }

  private boolean fileIsEmpty(File file) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      return br.readLine() == null;
    } catch (IOException ex) {
      LogContext.error(LOGGER, ex, "File is empty failed for file", file.getAbsolutePath());
    }
    return false;
  }

  @Override
  public void close() {}
}
