/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.github.mustachejava.MustacheResolver;
import io.dropwizard.views.View;

public interface MustacheResolverRegistry {
  MustacheResolver getResolverForClass(Class<? extends View> viewClass);
}
