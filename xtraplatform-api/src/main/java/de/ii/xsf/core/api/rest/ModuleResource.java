package de.ii.xsf.core.api.rest;

import de.ii.xsf.core.api.Module;
import de.ii.xsf.core.api.permission.AuthorizationProvider;

/**
 *
 * @author fischer
 */
public interface ModuleResource {
    
    public Module getModule();

    public void init(Module module, AuthorizationProvider permissions);
}
