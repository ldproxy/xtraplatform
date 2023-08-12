/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import de.ii.xtraplatform.web.domain.MustacheRenderer;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.views.common.ViewBundle;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class MustachePlugin implements DropwizardPlugin {
  private final MustacheRenderer mustacheRenderer;
  private final boolean isDevEnv;

  @Inject
  MustachePlugin(MustacheRenderer mustacheRenderer, AppContext appContext) {
    this.mustacheRenderer = mustacheRenderer;
    this.isDevEnv = appContext.isDevEnv();
  }

  @Override
  public void initBootstrap(Bootstrap<AppConfiguration> bootstrap) {
    boolean cacheTemplates = !isDevEnv;

    bootstrap.addBundle(
        new ViewBundle<>(ImmutableSet.of(mustacheRenderer)) {
          @Override
          public Map<String, Map<String, String>> getViewConfiguration(
              AppConfiguration configuration) {
            return ImmutableMap.of(
                mustacheRenderer.getConfigurationKey(),
                ImmutableMap.of("cache", Boolean.toString(cacheTemplates)));
          }
        });
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {}
}
