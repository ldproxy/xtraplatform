/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.firstrun;

import de.ii.xtraplatform.firstrun.api.FirstRunPage;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
@Component
@Provides(specifications={FirstRunPageRegistry.class})
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xsf.core.api.firstrun.FirstRunPage)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")

public class FirstRunPageRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirstRunPageRegistry.class);

    private final BundleContext context;

    private final List<FirstRunPage> pages;

    public FirstRunPageRegistry(@Context BundleContext context) {
        pages = new ArrayList<>();
        this.context = context;
    }

    public synchronized void onArrival(ServiceReference<FirstRunPage> ref) {
        FirstRunPage page = context.getService(ref);
        if (page != null) {
            pages.add(page);
            LOGGER.debug("Firstrun page registered: {}", page.getClass());
        }
    }

    public synchronized void onDeparture(ServiceReference<FirstRunPage> ref) {
        FirstRunPage page = context.getService(ref);
        if (page != null) {
            pages.remove(page);
            LOGGER.debug("Firstrun page unregistered: {}", page.getClass());
        }
    }

    public List<FirstRunPage> getPages() {
        return pages;
    }
}
