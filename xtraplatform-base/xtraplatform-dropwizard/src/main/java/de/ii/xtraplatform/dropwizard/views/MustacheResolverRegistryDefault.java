package de.ii.xtraplatform.dropwizard.views;

import com.github.mustachejava.MustacheResolver;
import io.dropwizard.views.View;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.dropwizard.views.PartialMustacheResolver)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class MustacheResolverRegistryDefault implements MustacheResolverRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(MustacheResolverRegistryDefault.class);

    private final BundleContext bundleContext;
    private final List<PartialMustacheResolver> partialMustacheResolvers;

    public MustacheResolverRegistryDefault(@Context BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.partialMustacheResolvers = new ArrayList<>();
    }

    @Override
    public MustacheResolver getResolverForClass(Class<? extends View> viewClass) {
        return templateName -> {
            Optional<PartialMustacheResolver> mustacheResolver = partialMustacheResolvers.stream()
                                                                                         .sorted(Comparator.comparing(PartialMustacheResolver::getSortPriority).reversed())
                                                                                         .filter(partialMustacheResolver -> partialMustacheResolver.canResolve(templateName, viewClass))
                                                                                         .findFirst();

            return mustacheResolver.map(partialMustacheResolver -> partialMustacheResolver.getReader(templateName, viewClass))
                                   .orElse(null);
        };
    }

    private synchronized void onArrival(ServiceReference<PartialMustacheResolver> ref) {
        try {
            final PartialMustacheResolver extension = bundleContext.getService(ref);
            int priority = extension.getSortPriority();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered partial mustache resolver with priority {}: {}", priority, extension);
            }

            partialMustacheResolvers.add(extension);
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onDeparture(ServiceReference<PartialMustacheResolver> ref) {
        final PartialMustacheResolver extension = bundleContext.getService(ref);

        if (Objects.nonNull(extension)) {

            partialMustacheResolvers.remove(extension);
        }
    }
}
