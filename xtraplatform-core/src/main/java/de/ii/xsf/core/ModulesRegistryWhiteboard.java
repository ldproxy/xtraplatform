/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.ii.xsf.core.api.Module;
import de.ii.xsf.core.api.ModulesRegistry;
import de.ii.xsf.logging.XSFLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.WebApplicationException;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author zahnen
 */

// TODO: remove
@Component
@Provides
@Instantiate
@Wbp(
  filter="(objectClass=de.ii.xsf.core.api.Module)", 
  onArrival="onArrival", 
  onDeparture="onDeparture")
public class ModulesRegistryWhiteboard implements ModulesRegistry{

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(ModulesRegistryWhiteboard.class);
    
    @Context
    private BundleContext context;
    
    private Map<String, Module> modules;
    private ObjectMapper jsonMapper;

    public ModulesRegistryWhiteboard() {
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        modules = new ConcurrentHashMap<String, Module>();

    }

    public synchronized void onArrival(ServiceReference<Module> ref) {
        Module mod = context.getService(ref);
        if (mod != null) {
            modules.put(mod.getName(), mod);
            LOGGER.getLogger().debug("Module started: {}", mod.getName());
        }
    }    
    public synchronized void onDeparture(ServiceReference<Module> ref) {
        Module mod = context.getService(ref);
        if (mod != null) {
            modules.remove(mod.getName());
            LOGGER.getLogger().debug("Module gone: {}", mod.getName());
        }
    }   
    
    @Override
    public Module getModule(String name, Class klass) {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            Module module = entry.getValue();
            if ((module.getName().equals(name) || module.getName().toLowerCase().equals(name)) && klass.isInstance(module)) {
                return module;
            }
        }
        //LOGGER.warn(FrameworkMessages.MODULE_NOT_FOUND, name);
        throw new WebApplicationException();
    }

    @Override
    public Module getModule(String name) {
        return getModule(name, Module.class);
    }

    @Override
    public Map<String, Module> getModules() {
        return modules;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }
}
