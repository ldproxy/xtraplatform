/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.web.domain.StaticResources;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StaticResourcesAuth implements StaticResources {

  @Inject
  StaticResourcesAuth() {}

  @Override
  public String getResourcePath() {
    return "/de/ii/xtraplatform/auth/assets";
  }

  @Override
  public String getUrlPath() {
    return "/xtraplatform-auth/assets";
  }
}
