package de.ii.xsf.core.server;

import de.ii.xsf.dropwizard.api.Dropwizard;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author zahnen
 *
 * see https://github.com/nxparser/nxparser/blob/master/nxparser-jax-rs/src/main/java/org/semanticweb/yars/jaxrs/trailingslash/RedirectMissingTrailingSlashFilter.java
 */
@Component
@Provides
@Instantiate
@PreMatching
public class NormalizeUriFilter implements ContainerRequestFilter {

    @Requires
    Dropwizard dropwizard;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        /*URI requestUri = requestContext.getUriInfo().getRequestUri();
        URI normalized = requestUri.normalize();
        String normalizedPath = normalized.getPath().replace("[^/]$", "/");

        if (!normalized.getPath().equals(normalizedPath)) {
            try {
                normalized = new URI(normalized.getScheme(), normalized.getAuthority(), normalizedPath, normalized.getQuery(), normalized.getFragment());
            } catch (URISyntaxException e) {
                // ignore
            }
        }

        if (!requestUri.equals(normalized))
            requestContext.setRequestUri(normalized);*/

        String path = requestContext.getUriInfo().getAbsolutePath().getPath();
        //if (path == null || !path.endsWith("/")) {
        if (path.endsWith("api")) {
            if (dropwizard.hasExternalUrl()) {
                requestContext.abortWith(
                        Response
                                .status(Response.Status.MOVED_PERMANENTLY)
                                .location(requestContext.getUriInfo().getRequestUriBuilder().replacePath(path.replace("rest/services/", "")+"/").build())
                                .build()
                );
                return;
            }

            requestContext.abortWith(
                    Response
                            .status(Response.Status.MOVED_PERMANENTLY)
                            .location(requestContext.getUriInfo().getRequestUriBuilder().path("/").build())
                            .build()
            );

        }
    }
}
