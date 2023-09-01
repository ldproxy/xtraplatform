/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CfgStoreDriverHttp implements CfgStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CfgStoreDriverHttp.class);
  private static final Path CFG_YML = Path.of("cfg.yml");
  private final Path tmpDir;

  public CfgStoreDriverHttp(Path tmpDir) {
    this.tmpDir = tmpDir;
  }

  @Override
  public Type getType() {
    return Type.HTTP;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    LOGGER.warn(
        "Content of type {} for {} is currently not supported.",
        Content.CFG,
        storeSource.getLabel());

    return true;
  }

  @Override
  public Optional<InputStream> load(StoreSource storeSource) throws IOException {
    return Optional.empty();
  }
}
