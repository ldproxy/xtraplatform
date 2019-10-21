package de.ii.xtraplatform.auth.internal;

import de.ii.xtraplatform.auth.api.TokenHandler;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class SplitCookieResponseFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitCookieResponseFilter.class);

    private final CoreServerConfig serverConfig;

    private final TokenHandler tokenHandler;

    public SplitCookieResponseFilter(@Requires CoreServerConfig serverConfig,
                                     @Requires TokenHandler tokenHandler) {
        this.serverConfig = serverConfig;
        this.tokenHandler = tokenHandler;
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        //TODO: would it be faster/easier to save needed information in context in SplitCookieCredentialAuthFilter and only read it here?

        boolean isAuthenticated = Optional.ofNullable(requestContext.getSecurityContext())
                                    .map(SecurityContext::getUserPrincipal).isPresent();

        int status = responseContext.getStatus();

        Optional<String> token = SplitCookie.readToken(requestContext.getCookies());

        //LOGGER.debug("RESPONSE {} {} {} {}", requestContext.getUriInfo().getRequestUri(), status, isAuthenticated, token);


        if (status == 200 && isAuthenticated && token.isPresent()) {
            boolean rememberMe = tokenHandler.parseTokenClaim(token.get(), "rememberMe", Boolean.class).orElse(false);

            List<String> authCookies = SplitCookie.writeToken(token.get(), getDomain(), isSecure(), rememberMe);

            authCookies.forEach(cookie -> responseContext.getHeaders().add("Set-Cookie", cookie));
        }
    }

    //TODO: instead of external url, get request url
    //TODO: but we want to access view action links with same token, would that work?
    private String getDomain() {
        return getExternalUri().getHost();
    }

    // TODO: even if external url is set, we might want to access manager via http://localhost
    private boolean isSecure() {
        return false;
        //return Objects.equals(getExternalUri().getScheme(), "https");
    }

    private URI getExternalUri() {
        return URI.create(serverConfig.getExternalUrl());
    }
}
