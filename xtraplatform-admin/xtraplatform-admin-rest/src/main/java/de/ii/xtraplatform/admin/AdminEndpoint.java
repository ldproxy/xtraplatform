/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.admin;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.api.MediaTypeCharset;
import de.ii.xtraplatform.api.exceptions.ResourceNotFound;
import de.ii.xtraplatform.cfgstore.api.LocalBundleConfigStore;
import de.ii.xtraplatform.web.api.Endpoint;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Whiteboards(whiteboards = {
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.service.api.AdminServiceResource)",
                onArrival = "onServiceResourceArrival",
                onDeparture = "onServiceResourceDeparture")
})

@Path("/admin/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class AdminEndpoint implements Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpoint.class);

    private String xsfVersion = "todo";


    @GET
    @CacheControl(noCache = true)
    public AdminRoot getAdmin() {
        return new AdminRoot(xsfVersion);
    }

    @Path("/servicetypes")
    @GET
    @CacheControl(noCache = true)
    public Collection getAdminServiceTypes() {
        return ImmutableList.of();//TODO serviceRegistry.getServiceTypes();
    }


    @Requires
    LocalBundleConfigStore localBundleConfigStore;

    @Path("/settings")
    @GET
    @CacheControl(noCache = true)
    public Map<String, Object> getSettingCategories(/*@Auth AuthenticatedUser authUser*/) {
        return localBundleConfigStore.getCategories();
    }

    @Path("/settings/{category}")
    @GET
    @CacheControl(noCache = true)
    public Map<String, Object> getSettingCategory(/*@Auth AuthenticatedUser authUser,*/ @PathParam("category") String category) {
        if (!localBundleConfigStore.hasCategory(category)) {
            throw new ResourceNotFound();
        }

        return localBundleConfigStore.getConfigProperties(category);
    }

    @Path("/settings/{category}")
    @POST
    @CacheControl(noCache = true)
    public Map<String, Object> postSettingCategory(/*@Auth AuthenticatedUser authUser,*/ @PathParam("category") String category, Map<String, String> body) {
        if (!localBundleConfigStore.hasCategory(category)) {
            throw new ResourceNotFound();
        }

        try {
            localBundleConfigStore.updateConfigProperties(category, body);
        } catch (IOException e) {
            throw new NotAcceptableException();
        }

        return localBundleConfigStore.getConfigProperties(category);
    }

}
