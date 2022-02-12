/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.web.domain.AbstractConfigurationProvider;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ConfigurationProviderDefault
    extends AbstractConfigurationProvider {

  @Inject
  public ConfigurationProviderDefault() {
  }

  @Override
  public Class<XtraPlatformConfiguration> getConfigurationClass() {
    return XtraPlatformConfiguration.class;
  }
}
