/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.io.Reader;

@AutoMultiBind
public interface PartialMustacheResolver {

  int getSortPriority();

  boolean canResolve(String templateName, Class<?> viewClass);

  Reader getReader(String templateName, Class<?> viewClass);
}
