package de.ii.xtraplatform.dropwizard.app;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.extender.Extender;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Instantiate
@Extender(
        onArrival="onBundleArrival",
        onDeparture="onBundleDeparture",
        extension="iPOJO-Components")
public class JacksonSubTypeRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonSubTypeRegistry.class);

    private synchronized void onBundleArrival(Bundle bundle, String header) {
     LOGGER.debug("BUNDLE {} {}", bundle.getSymbolicName(), bundle.getVersion());
    }

    private synchronized void onBundleDeparture(Bundle bundle) {

    }
}
