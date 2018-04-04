/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.rest;

import de.ii.xsf.core.api.Module;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import io.dropwizard.jersey.caching.CacheControl;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 *
 * @author fischer
 */
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class AdminModuleResource implements ModuleResource {

    protected Module module = null;
    protected AuthorizationProvider permissions;
    
    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public void init(Module module, AuthorizationProvider permissions) {
        this.module = module;
        this.permissions = permissions;
    }

    @GET
    @CacheControl(noCache = true, mustRevalidate = true)
    public AdminModuleWrapper getAdminModule() {
        return new AdminModuleWrapper(module);
    }
}
