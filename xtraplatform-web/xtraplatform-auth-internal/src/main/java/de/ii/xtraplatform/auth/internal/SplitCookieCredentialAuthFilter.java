package de.ii.xtraplatform.auth.internal;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

@Priority(Priorities.AUTHENTICATION)
public class SplitCookieCredentialAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitCookieCredentialAuthFilter.class);

    private SplitCookieCredentialAuthFilter() {
        this.prefix = "Bearer";
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        String credentials = SplitCookie.readToken(requestContext.getCookies())
                                        .orElse(null);

        LOGGER.debug("credentials {}", credentials);

        if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
        }
    }

    /**
     * Builder for {@link SplitCookieCredentialAuthFilter}.
     * <p>An {@link Authenticator} must be provided during the building process.</p>
     *
     * @param <P> the type of the principal
     */
    public static class Builder<P extends Principal>
            extends AuthFilter.AuthFilterBuilder<String, P, SplitCookieCredentialAuthFilter<P>> {

        @Override
        protected SplitCookieCredentialAuthFilter<P> newInstance() {
            return new SplitCookieCredentialAuthFilter<>();
        }
    }
}
