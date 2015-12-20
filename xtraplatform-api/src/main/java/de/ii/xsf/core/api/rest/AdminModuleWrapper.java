package de.ii.xsf.core.api.rest;

import de.ii.xsf.core.api.Module;

/**
 *
 * @author zahnen
 */
public class AdminModuleWrapper {

    private Module module;

    public AdminModuleWrapper(Module module) {
        this.module = module;
    }

    public String getName() {
        return module.getName();
    }

    public String getDescription() {
        return module.getDescription();
    }
}
