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
package de.ii.xsf.cfgstore.api.handler;

import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.ConfigurationListenerRegistry;
import de.ii.xsf.cfgstore.api.JsonBundleConfig;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.util.Dictionary;

/**
 *
 * @author zahnen
 */
public class BundleConfigHandler extends PrimitiveHandler {

    protected static final LocalizedLogger LOGGER = XSFLogger.getLogger(BundleConfigHandler.class);
    public static final String NAMESPACE = "de.ii.xsf.cfgstore.api.handler";//BundleConfigHandler.class.getPackage().getName();

    protected ConfigurationListenerRegistry clr;
    protected ResourceStore<JsonBundleConfig> store;
    private InstanceManager instanceManager;


    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        if (!getPojoMetadata().getSuperClass().equals(BundleConfigDefault.class.getName())) {
            throw new ConfigurationException("The class " + getPojoMetadata().getClassName() + " does not extend " + BundleConfigDefault.class.getName());
        }

        instanceManager = getInstanceManager();
    }

    @Override
    public void onCreation(Object instance) {
        super.onCreation(instance);

        try {
            ((BundleConfigDefault) instance).init(instanceManager.getContext().getBundle().getSymbolicName(), instanceManager.getClassName(), store, clr);
        } catch (IOException ex) {
            LOGGER.getLogger().error("The component instance {} failed", instance, ex);
            this.stop();
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
