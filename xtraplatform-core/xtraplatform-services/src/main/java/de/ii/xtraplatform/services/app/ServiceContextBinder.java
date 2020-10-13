/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceInjectableContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;

/** @author zahnen */
@Component
@Provides
@Instantiate
@Provider
public class ServiceContextBinder extends AbstractBinder
    implements Binder, ServiceInjectableContext {

  // TODO: bind every subtype
  @Override
  protected void configure() {
    bindFactory(ServiceFactory.class).proxy(true).proxyForSameScope(false).to(Service.class).in(RequestScoped.class);
  }

  @Override
  public void inject(ContainerRequestContext containerRequestContext, Service service) {
    containerRequestContext.setProperty(ServiceInjectableContext.SERVICE_CONTEXT_KEY, service);
  }

  public static class ServiceFactory extends AbstractContainerRequestValueFactory<Service> {

    @Override
    @RequestScoped
    public Service provide() {
      return (Service)
          getContainerRequest().getProperty(ServiceInjectableContext.SERVICE_CONTEXT_KEY);
    }
  }
}