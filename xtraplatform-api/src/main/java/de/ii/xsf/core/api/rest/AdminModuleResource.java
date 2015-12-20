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
