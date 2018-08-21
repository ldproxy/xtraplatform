package de.ii.xtraplatform.service.test;

import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.service.api.AbstractService;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.PostRegistration;
import org.apache.felix.ipojo.annotations.PostUnregistration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Component
@Provides
@Entity(entityType = Service.class, dataType = ServiceTestData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
@HandlerDeclaration("<properties></properties>")
public class ServiceTest extends AbstractService<ServiceTestData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTest.class);

    @Validate
    void onStart() {
        LOGGER.debug("STARTED {} {}", getId(), shouldRegister());
    }

    @PostRegistration
    void onPublish(ServiceReference ref) {
        LOGGER.debug("PUBLISHED {} ", getId());
    }

    @Invalidate
    void onStop() {
        LOGGER.debug("STOPPED {}", getId());
    }

    @PostUnregistration
    void onHide(ServiceReference ref) {
        LOGGER.debug("HIDDEN {}", getId());
    }

    @Override
    protected ImmutableServiceTestData dataToImmutable(ServiceTestData data) {
        return ImmutableServiceTestData.copyOf(data);
    }
}
