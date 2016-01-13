/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
