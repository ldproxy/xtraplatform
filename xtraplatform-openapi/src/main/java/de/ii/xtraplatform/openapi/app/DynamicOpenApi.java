/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.openapi.app;

import io.swagger.v3.core.filter.OpenAPISpecFilter;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface DynamicOpenApi {

  Response getOpenApi(
      HttpHeaders headers, UriInfo uriInfo, String type, OpenAPISpecFilter specFilter);

  Response getOpenApi(
      HttpHeaders headers, ServletConfig config, Application app, UriInfo uriInfo, String yaml);
}
