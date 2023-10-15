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
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Store;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.blobs.domain.BlobStoreDriver;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ResourceStoreImpl extends BlobStore implements ResourceStore, AppLifeCycle {

  @Inject
  public ResourceStoreImpl(Store store, Lazy<Set<BlobStoreDriver>> drivers) {
    super(store, drivers, Content.RESOURCES);
  }

  @Override
  public int getPriority() {
    return 50;
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public CompletableFuture<Void> onReady() {
    return super.onReady();
  }

  @Override
  public Path getPrefix() {
    return Path.of("");
  }
}
