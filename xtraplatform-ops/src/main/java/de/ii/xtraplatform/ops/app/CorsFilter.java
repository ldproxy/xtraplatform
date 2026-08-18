/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.function.BiConsumer;

@Provider
public class CorsFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    addCorsHeaders(responseContext.getHeaders()::add);
  }

  public static void addCorsHeaders(HttpServletResponse response) {
    addCorsHeaders(response::addHeader);
  }

  // Credentials are not allowed *together with the wildcard origin*: a browser rejects a
  // credentialed cross-origin response whose Access-Control-Allow-Origin is "*", so the pair
  // grants nothing that works, while it pre-arms the classic misconfiguration — the moment the
  // wildcard is replaced by a reflected origin, any site a victim visits can call these
  // endpoints with the victim's cookies and read the answers.
  //
  // Note what "credentials" means here: cookies, HTTP authentication and TLS client
  // certificates. A bearer token that a client sets explicitly is an ordinary request header
  // (allowed below), so a token-authenticated client needs nothing from this flag. The
  // dashboard is served from the same origin as this API, so CORS does not apply to it at all;
  // the wildcard remains only for external tooling against the documented OpenAPI endpoint, and
  // is tenable only because these endpoints are not meant to be reachable from outside the
  // deployment.
  //
  // If a cross-origin browser client ever has to authenticate with cookies or Basic auth, the
  // correct change is to allow-list its origin (echoing only known origins, with Vary: Origin)
  // and set Allow-Credentials for those — never to combine credentials with "*".
  private static void addCorsHeaders(BiConsumer<String, String> headers) {
    headers.accept("Access-Control-Allow-Origin", "*");
    headers.accept("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
    headers.accept("Access-Control-Allow-Methods", "GET, POST");
  }
}
