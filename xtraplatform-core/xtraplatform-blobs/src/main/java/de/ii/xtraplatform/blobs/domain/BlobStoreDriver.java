/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.base.domain.StoreDriver;
import de.ii.xtraplatform.base.domain.StoreSource;
import java.io.IOException;

@AutoMultiBind
public interface BlobStoreDriver extends StoreDriver {

  BlobSource init(StoreSource storeSource) throws IOException;
}
