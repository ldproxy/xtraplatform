/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.domain;

import com.google.common.io.ByteSource;
import de.ii.xtraplatform.runtime.domain.Constants;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;

//@AutoMultiBind
public interface ApplicationProvider {

  Class<XtraPlatformConfiguration> getConfigurationClass();

  Optional<ByteSource> getConfigurationFileTemplate(String environment);

  Pair<XtraPlatformConfiguration, Environment> startWithFile(
      Path configurationFile, Constants.ENV env, Consumer<Bootstrap<XtraPlatformConfiguration>> initializer);
}
