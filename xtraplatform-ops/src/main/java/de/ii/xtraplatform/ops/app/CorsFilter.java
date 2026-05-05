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

  private static void addCorsHeaders(BiConsumer<String, String> headers) {
    headers.accept("Access-Control-Allow-Origin", "*");
    headers.accept("Access-Control-Allow-Credentials", "true");
    headers.accept("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
    headers.accept("Access-Control-Allow-Methods", "GET, POST");
  }
}
