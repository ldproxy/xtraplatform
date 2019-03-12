/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cfgstore.api.handler;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cfgstore.api.BundleConfigDefault;
import de.ii.xtraplatform.cfgstore.api.BundleConfigStore;
import de.ii.xtraplatform.cfgstore.api.ConfigurationListenerRegistry;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
public class BundleConfigHandler extends PrimitiveHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleConfigHandler.class);
    public static final String NAMESPACE = "de.ii.xtraplatform.cfgstore.api.handler";//BundleConfigHandler.class.getPackage().getName();

    protected ConfigurationListenerRegistry clr;
    protected BundleConfigStore store;
    protected String annotationName;
    private InstanceManager instanceManager;
    private String category;
    private Map<String, Map<String, String>> properties;
    private Optional<String> bundleId;


    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        if (!getPojoMetadata().getSuperClass().equals(BundleConfigDefault.class.getName())) {
            throw new ConfigurationException("The class " + getPojoMetadata().getClassName() + " does not extend " + BundleConfigDefault.class.getName());
        }

        Element[] bundleConfigElements = metadata.getElements(annotationName, NAMESPACE);

        if (bundleConfigElements[0].containsAttribute("category")) {
            this.category = bundleConfigElements[0].getAttribute("category");
        }
        if (bundleConfigElements[0].containsElement("configpropertydescriptor", "de.ii.xtraplatform.cfgstore.api")) {
            parseProperties(bundleConfigElements[0].getElements("configpropertydescriptor", "de.ii.xtraplatform.cfgstore.api"));
        }
        this.bundleId = Optional.ofNullable(Strings.emptyToNull(bundleConfigElements[0].getAttribute("bundleId")));

        instanceManager = getInstanceManager();
    }

    private void parseProperties(final Element[] properties) {
        final ImmutableMap.Builder<String, Map<String, String>> builder = ImmutableMap.builder();

        for (Element property: properties) {
            final ImmutableMap.Builder<String, String> builder1 = ImmutableMap.builder();

            for (Attribute attribute: property.getAttributes()) {
                builder1.put(attribute.getName(), attribute.getValue().replaceAll("''", "\""));
            }

            builder.put(property.getAttribute("name"), builder1.build());
        }

        this.properties = builder.build();
    }

    @Override
    public void onCreation(Object instance) {
        super.onCreation(instance);

        try {
            ((BundleConfigDefault) instance).init(bundleId.orElse(instanceManager.getContext().getBundle().getSymbolicName()), instanceManager.getClazz().getSimpleName(), store, clr, category, properties);
        } catch (IOException ex) {
            LOGGER.error("The component instance {} failed", instance, ex);
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
