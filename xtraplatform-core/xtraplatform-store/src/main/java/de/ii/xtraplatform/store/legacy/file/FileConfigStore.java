/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.file;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import de.ii.xtraplatform.store.domain.legacy.KeyNotFoundException;
import de.ii.xtraplatform.store.domain.legacy.KeyValueStore;
import de.ii.xtraplatform.store.legacy.Transaction;
import de.ii.xtraplatform.store.legacy.WriteTransaction;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author zahnen */
public class FileConfigStore implements KeyValueStore {

  private static final String INDEX_FILE_NAME = "index.properties";
  protected static final String ENCODING = "UTF-8";

  protected final File rootDir;

  public FileConfigStore(File rootDir) {
    this.rootDir = rootDir;
  }

  @Override
  public WriteTransaction<String> openWriteTransaction(String id) {
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }
    return new WriteFileTransaction(getFile(id));
  }

  @Override
  public Transaction openDeleteTransaction(String id) {
    return new DeleteFileTransaction(getFile(id));
  }

  @Override
  public Reader getValueReader(String id) throws KeyNotFoundException, IOException {
    File cfg = getFile(id);
    if (!cfg.isFile()) {
      throw new KeyNotFoundException();
    }
    // FileReader uses system encoding, we don't want that
    return new InputStreamReader(new FileInputStream(cfg), ENCODING);
  }

  @Override
  public boolean containsKey(String id) {
    File cfg = getFile(id);

    return cfg.exists();
  }

  private static final HashFunction hashFunction = Hashing.goodFastHash(128);

  private File getFile(String id) {
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }

    if (id.contains("/") || id.contains("\\") || id.contains(":")) {
      String hash = hashFunction.hashString(id, StandardCharsets.UTF_8).toString();

      addToIndex(id, hash);
      id = hash;
    }

    return new File(rootDir, id);
  }

  private void addToIndex(String origid, String hash) {

    // System.out.println(origid + " - " + hash);
    try {
      Properties props = new Properties();

      if (new File(rootDir, INDEX_FILE_NAME).exists()) {

        FileInputStream in = new FileInputStream(rootDir + "/" + INDEX_FILE_NAME);
        props.load(in);
        in.close();
      }

      props.setProperty(hash, origid);
      FileOutputStream out = new FileOutputStream(rootDir + "/" + INDEX_FILE_NAME);
      props.store(out, null);
    } catch (IOException ex) {
      Logger.getLogger(FileConfigStore.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public List<String> getKeys() {

    Properties props = new Properties();
    if (new File(rootDir, INDEX_FILE_NAME).exists()) {
      try {

        FileInputStream in = new FileInputStream(rootDir + "/" + INDEX_FILE_NAME);
        props.load(in);

        in.close();
      } catch (IOException ex) {
        Logger.getLogger(FileConfigStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    List<String> files = new ArrayList<>();
    if (rootDir.exists()) {
      for (File f : rootDir.listFiles()) {
        if (f.isFile()
            && !f.getAbsolutePath().endsWith(INDEX_FILE_NAME)
            && !f.getAbsolutePath().endsWith("-custom")
            && !f.getAbsolutePath().endsWith("-config")
            && !f.getAbsolutePath().endsWith("-backup")
            && !f.getName().startsWith(".")) {

          if (!props.isEmpty()) {
            files.add(props.getProperty(f.getName()));
          } else {
            files.add(f.getName());
          }
        }
      }
    }
    return files;
  }

  @Override
  public KeyValueStore getChildStore(String... path) {
    File configStore = new File(rootDir, path[0]);

    for (int i = 1; i < path.length; i++) {
      configStore = new File(configStore, path[i]);
    }

    /*if (!configStore.exists()) {
        configStore.mkdirs();
    }*/
    return new FileConfigStore(configStore);
  }

  @Override
  public List<String> getChildStoreIds() {
    List<String> files = new ArrayList<>();
    for (File f : rootDir.listFiles()) {
      if (f.isDirectory()) {
        files.add(f.getName());
      }
    }
    return files;
  }
}
