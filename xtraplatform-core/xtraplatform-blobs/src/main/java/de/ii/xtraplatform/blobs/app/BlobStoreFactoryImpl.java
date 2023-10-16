/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.Store;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.blobs.domain.BlobStoreDriver;
import de.ii.xtraplatform.blobs.domain.BlobStoreFactory;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class BlobStoreFactoryImpl implements BlobStoreFactory {

  private final Store store;
  private final Lazy<Set<BlobStoreDriver>> drivers;

  @Inject
  BlobStoreFactoryImpl(Store store, Lazy<Set<BlobStoreDriver>> drivers) {
    this.store = store;
    this.drivers = drivers;
  }

  @Override
  public BlobStore createBlobStore(Content contentType) {
    return new BlobStoreImpl(store, drivers, contentType);
  }
}
