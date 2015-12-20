package de.ii.xsf.core.firstrun;

import de.ii.xsf.core.api.firstrun.FirstRunPage;
import de.ii.xsf.logging.XSFLogger;
import java.util.ArrayList;
import java.util.List;
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

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FirstRunPageRegistry.class);
    
    @Context
    private BundleContext context;

    private final List<FirstRunPage> pages;

    public FirstRunPageRegistry() {
        pages = new ArrayList<>();
    }

    public synchronized void onArrival(ServiceReference<FirstRunPage> ref) {
        FirstRunPage page = context.getService(ref);
        if (page != null) {
            pages.add(page);
            LOGGER.getLogger().debug("Firstrun page registered: {}", page.getClass());
        }
    }

    public synchronized void onDeparture(ServiceReference<FirstRunPage> ref) {
        FirstRunPage page = context.getService(ref);
        if (page != null) {
            pages.remove(page);
            LOGGER.getLogger().debug("Firstrun page unregistered: {}", page.getClass());
        }
    }

    public List<FirstRunPage> getPages() {
        return pages;
    }
}
