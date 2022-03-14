/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import dagger.BindsInstance;
import dagger.Lazy;
import java.util.Set;

public interface App {

  AppContext appContext();

  Lazy<Set<AppLifeCycle>> lifecycle();

  interface Builder {
    @BindsInstance
    Builder appContext(AppContext appContext);

    App build();
  }
}
