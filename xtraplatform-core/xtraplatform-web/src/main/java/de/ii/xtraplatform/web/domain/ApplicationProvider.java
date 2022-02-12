/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import de.ii.xtraplatform.base.domain.Constants;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;

//@AutoMultiBind
public interface ApplicationProvider {

  Pair<AppConfiguration, Environment> startWithFile(
      Path configurationFile, Constants.ENV env, Consumer<Bootstrap<AppConfiguration>> initializer);
}
