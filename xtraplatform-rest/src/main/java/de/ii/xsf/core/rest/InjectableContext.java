package de.ii.xsf.core.rest;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * @author zahnen
 */
public interface InjectableContext<T> {
    void inject(ContainerRequestContext requestContext, T injectable);
}
