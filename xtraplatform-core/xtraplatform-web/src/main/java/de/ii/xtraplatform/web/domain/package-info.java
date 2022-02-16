@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {ContainerRequestFilter.class, ContainerResponseFilter.class, Binder.class})
package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import org.glassfish.jersey.internal.inject.Binder;
