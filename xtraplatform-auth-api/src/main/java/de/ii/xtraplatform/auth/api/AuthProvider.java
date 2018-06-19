package de.ii.xtraplatform.auth.api;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.security.Principal;

/**
 * @author zahnen
 */
public interface AuthProvider<T extends Principal> {
    AuthDynamicFeature getAuthDynamicFeature();

    default Class<?> getRolesAllowedDynamicFeature() {
        return RolesAllowedDynamicFeature.class;
    }

    AuthValueFactoryProvider.Binder<T> getAuthValueFactoryProvider();
}
