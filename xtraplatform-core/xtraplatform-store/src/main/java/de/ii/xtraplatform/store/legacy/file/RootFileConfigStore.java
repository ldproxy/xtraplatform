/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.file;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.store.domain.legacy.KeyValueStoreLegacy;
import javax.inject.Inject;
import javax.inject.Singleton;

/** @author zahnen */
@Singleton
@AutoBind
public class RootFileConfigStore extends FileConfigStore implements KeyValueStoreLegacy {

  private static final String ROOT_DIR_NAME = "store";

  @Inject
  public RootFileConfigStore(AppContext appContext) {
    super(appContext.getDataDir().resolve(ROOT_DIR_NAME).toFile());

    if (!rootDir.exists()) {
      // rootDir.mkdirs();
    }
    if (!rootDir.isDirectory()) {
      // TODO
      // throw exception
    }
  }
}
