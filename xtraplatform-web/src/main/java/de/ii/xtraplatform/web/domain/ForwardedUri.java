/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.google.common.base.Splitter;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;

public interface ForwardedUri {

  String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";
  Splitter PATH_SPLITTER = Splitter.on('/').trimResults().omitEmptyStrings();

  static URICustomizer from(ContainerRequestContext requestContext) {
    return new URICustomizer(requestContext.getUriInfo().getRequestUri())
        .prependPathSegments(prefix(requestContext));
  }

  static URICustomizer base(ContainerRequestContext requestContext) {
    return new URICustomizer(requestContext.getUriInfo().getRequestUri())
        .setPathSegments(prefix(requestContext))
        .ensureNoTrailingSlash()
        .clearParameters();
  }

  static List<String> prefix(ContainerRequestContext requestContext) {
    if (requestContext.getHeaders().containsKey(X_FORWARDED_PREFIX)) {
      return PATH_SPLITTER.splitToList(requestContext.getHeaderString(X_FORWARDED_PREFIX));
    }
    return List.of();
  }
}
