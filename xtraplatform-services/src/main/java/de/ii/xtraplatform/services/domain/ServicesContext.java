/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import de.ii.xtraplatform.base.domain.WebContext;
import de.ii.xtraplatform.web.domain.URICustomizer;

public interface ServicesContext extends WebContext {

  default String getApiUri(ServiceData serviceData) {
    return new URICustomizer(getUri())
        .ensureLastPathSegments(serviceData.getSubPath().toArray(String[]::new))
        .toString();
  }
}
