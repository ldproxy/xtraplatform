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
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.internal.inject.Binder;
