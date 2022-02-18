/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.manager.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.MediaTypeCharset;
import io.dropwizard.jersey.caching.CacheControl;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Singleton
@AutoBind
@Path("/admin/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class AdminEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpoint.class);

  private final String version;

  @Inject
  public AdminEndpoint(AppContext appContext) {
    this.version = appContext.getVersion();
  }

  @GET
  @CacheControl(noCache = true)
  public AdminRoot getAdmin() {
    return new AdminRoot(version);
  }

  @Path("/servicetypes")
  @GET
  @CacheControl(noCache = true)
  public Collection getAdminServiceTypes() {
    return ImmutableList.of(); // TODO serviceRegistry.getServiceTypes();
  }
}
