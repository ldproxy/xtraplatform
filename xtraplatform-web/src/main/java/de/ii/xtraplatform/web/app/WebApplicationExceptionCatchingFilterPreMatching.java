/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import java.io.IOException;
import java.util.Objects;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

@PreMatching
public class WebApplicationExceptionCatchingFilterPreMatching implements ContainerRequestFilter {
  private final ContainerRequestFilter underlying;

  public WebApplicationExceptionCatchingFilterPreMatching(ContainerRequestFilter underlying) {
    Objects.requireNonNull(underlying, "Underlying ContainerRequestFilter is not set");
    this.underlying = underlying;
  }

  @Override
  @SuppressWarnings("PMD.EmptyCatchBlock")
  public void filter(ContainerRequestContext requestContext) throws IOException {
    try {
      this.underlying.filter(requestContext);
    } catch (WebApplicationException var3) {
    }
  }
}
