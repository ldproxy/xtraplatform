package de.ii.xsf.core.api;

import java.util.Map;

/**
 *
 * @author zahnen
 */
public interface ModulesRegistry {
    public Map<String, Module> getModules();
    public Module getModule(String name, Class klass);
    public Module getModule(String name);
}
