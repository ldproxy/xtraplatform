/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceInjectableContext;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.process.internal.RequestScoped;

/** @author zahnen */
@Singleton
@AutoBind
@Provider
public class ServiceContextBinder extends AbstractBinder
    implements Binder, ServiceInjectableContext {

  @Inject
  public ServiceContextBinder() {}

  // TODO: bind every subtype
  @Override
  protected void configure() {
    bindFactory(ServiceFactory.class)
        .proxy(true)
        .proxyForSameScope(false)
        .to(Service.class)
        .in(RequestScoped.class);
  }

  @Override
  public void inject(ContainerRequestContext containerRequestContext, Service service) {
    containerRequestContext.setProperty(ServiceInjectableContext.SERVICE_CONTEXT_KEY, service);
  }

  public static class ServiceFactory implements Supplier<Service> {

    private final ContainerRequestContext containerRequestContext;

    @Inject
    public ServiceFactory(ContainerRequestContext containerRequestContext) {
      this.containerRequestContext = containerRequestContext;
    }

    @Override
    public Service get() {
      return (Service)
          containerRequestContext.getProperty(ServiceInjectableContext.SERVICE_CONTEXT_KEY);
    }
  }
}
