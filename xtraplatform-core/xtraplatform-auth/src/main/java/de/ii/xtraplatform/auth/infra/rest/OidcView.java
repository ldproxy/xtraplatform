/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import com.google.common.base.Charsets;
import io.dropwizard.views.common.View;

public class OidcView extends View {
  public final String oidcUri;
  public final String callbackUri;
  public final String redirectUri;
  public final String clientId;
  public final String clientSecret;
  public final boolean callback;

  public OidcView(
      String oidcUri,
      String callbackUri,
      String redirectUri,
      String clientId,
      String clientSecret,
      boolean callback) {
    super("/templates/oidc.mustache", Charsets.UTF_8);
    this.oidcUri = oidcUri;
    this.callbackUri = callbackUri;
    this.redirectUri = redirectUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.callback = callback;
  }
}
