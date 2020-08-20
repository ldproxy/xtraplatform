/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web;

import de.ii.xtraplatform.web.amdatu.DefaultPageParser;
import de.ii.xtraplatform.web.amdatu.DefaultPages;
import de.ii.xtraplatform.web.amdatu.InvalidEntryException;
import de.ii.xtraplatform.web.amdatu.ResourceEntry;
import de.ii.xtraplatform.web.amdatu.ResourceKeyParser;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.extender.Extender;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author zahnen
 */
@Component
//@Provides(specifications=StaticResourceRegistry.class)
@Instantiate
@Extender(
    onArrival="onBundleArrival",
    onDeparture="onBundleDeparture",
    extension= StaticResourceConstants.WEB_RESOURCE_KEY)
public class StaticResourceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceRegistry.class);
    

    private final BundleContext bundleContext;
    private final Map<Long, List<ServiceRegistration>> servlets;

    public StaticResourceRegistry(@Context BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.servlets = new ConcurrentHashMap<>();
    }
    
    private synchronized void onBundleArrival(Bundle bundle, String header) {
        try {
            Map<String, ResourceEntry> entryMap = ResourceKeyParser.getEntries(header);
            DefaultPages defaultPages = DefaultPageParser.parseDefaultPages(bundle.getHeaders().get(StaticResourceConstants.WEB_RESOURCE_DEFAULT_PAGE));
            List<ServiceRegistration> serviceRegistrations = new ArrayList<>();
            
            for (ResourceEntry entry : entryMap.values()) {
                LOGGER.debug("Registered static web resource: {} {} {}", entry.getPaths().get(0), entry.getAlias(), defaultPages.getDefaultPageFor(entry.getPaths().get(0)));

                HttpServlet staticResourceServlet = new StaticResourceServlet(entry.getPaths().get(0), entry.getAlias(), StandardCharsets.UTF_8, bundle, defaultPages);
                Hashtable<String, Object> props = new Hashtable<>();
                props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, entry.getAlias());
                props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                        "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.osgi.service.http)");

                ServiceRegistration serviceRegistration = bundleContext.registerService(Servlet.class.getName(), staticResourceServlet, props);
                serviceRegistrations.add(serviceRegistration);
            }
            servlets.put(bundle.getBundleId(), serviceRegistrations);
            
        } catch (InvalidEntryException ex) {
            //LOGGER.info("STATIC", ex);
        }
    }

    private synchronized void onBundleDeparture(Bundle bundle) {
         if (servlets.containsKey(bundle.getBundleId())) {
             for (ServiceRegistration serviceRegistration: servlets.get(bundle.getBundleId())) {
                serviceRegistration.unregister();
             }
         }
    }
}
