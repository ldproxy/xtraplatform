@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {
      ContainerRequestFilter.class,
      ContainerResponseFilter.class,
      Binder.class,
      ExceptionMapper.class
    })
package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.internal.inject.Binder;
