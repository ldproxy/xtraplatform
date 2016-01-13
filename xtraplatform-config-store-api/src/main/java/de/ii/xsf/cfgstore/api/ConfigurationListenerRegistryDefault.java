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
package de.ii.xsf.cfgstore.api;

import de.ii.xsf.logging.XSFLogger;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author zahnen
 */
//@Configuration
@Component
@Provides
@Instantiate
@Whiteboards(whiteboards = {
    @Wbp(
            filter = "(objectClass=de.ii.xsf.cfgstore.api.ConfigurationListener)",
            onArrival = "onConfigurationListenerArrival",
            onDeparture = "onConfigurationListenerDeparture")
})
public class ConfigurationListenerRegistryDefault implements ConfigurationListenerRegistry {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(ConfigurationListenerRegistryDefault.class);

    @Context
    BundleContext context;

    // TODO: cleanup

    //private final Map<String, CCReg> registry;
    private final Map<Class, List<ConfigurationListener>> registry;

    public ConfigurationListenerRegistryDefault() {
        registry = new HashMap<>();
    }

    /*private synchronized CCReg getReg(String id) {
     if (!registry.containsKey(id)) {
     registry.put(id, new CCReg(id));
     }
     return registry.get(id);
     }

     public synchronized void onBundleFactoryArrival(ServiceReference<Factory> ref) {
     Factory fac = context.getService(ref);
     String bundle = ref.getBundle().getSymbolicName();

     for (String intrfc : fac.getComponentDescription().getprovidedServiceSpecification()) {
     if (intrfc.equals(ConfigurationListener.class.getName())) {
     LOGGER.getLogger().info("FACTORY {} {} {}", fac.getClassName(), bundle, fac.getState());

     CCReg reg = getReg(fac.getClassName());
     reg.setFactory(fac);
     reg.tryInstantiate();
     }
     }

     }

     public synchronized void onBundleConfigArrival(ServiceReference<BundleConfig> ref) {
     BundleConfig bc = context.getService(ref);
     String bundle = ref.getBundle().getSymbolicName();

     LOGGER.getLogger().info("CONFIG {} {}", bc.getResourceId(), bundle);

     CCReg reg = getReg(bc.getResourceId());
     reg.setConfig(bc);
     reg.tryInstantiate();

     }*/
    private synchronized void onConfigurationListenerArrival(ServiceReference<ConfigurationListener> ref) {
        ConfigurationListener cl = context.getService(ref);
        String bundle = ref.getBundle().getSymbolicName();

        // Get the Generics Parameter Class
        for (Type t : cl.getClass().getGenericInterfaces()) {
            if (ConfigurationListener.class == (Class) ((ParameterizedType) t).getRawType()) {
                Class configClass = (Class) ((ParameterizedType) t).getActualTypeArguments()[0];

                addListener(configClass, cl);
            }
        }

        //LOGGER.getLogger().info("CONFIGLISTENER {} {}", cl.getClass().getName(), c.getName());
    }

    private void addListener(Class configClass, ConfigurationListener cl) {
        if (!registry.containsKey(configClass)) {
            registry.put(configClass, new ArrayList<ConfigurationListener>());
        }

        registry.get(configClass).add(cl);
        LOGGER.getLogger().info("CONFIGLISTENER: {} is listening for configuration {}", cl.getClass().getName(), configClass.getName());
    }

    @Override
    public void update(BundleConfigDefault cfg) {
        if (registry.containsKey(cfg.getClass())) {
            for (ConfigurationListener cl : registry.get(cfg.getClass())) {
                cl.onConfigurationUpdate(cfg);
                LOGGER.getLogger().info("CONFIGLISTENER: updating {}", cl.getClass().getName());
            }
        }
    }
}
