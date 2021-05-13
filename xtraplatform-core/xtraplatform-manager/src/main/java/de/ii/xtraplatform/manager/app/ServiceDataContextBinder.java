/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.manager.app;

import de.ii.xtraplatform.services.domain.ServiceData;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.process.internal.RequestScoped;

/** @author zahnen */
@Component
@Provides
@Instantiate
@Provider
public class ServiceDataContextBinder extends AbstractBinder
    implements Binder, ServiceDataInjectableContext {

  public static final String SERVICE_DATA_CONTEXT_KEY = "XP_SERVICE_DATA";

  // TODO: bind every subtype
  @Override
  protected void configure() {
    bindFactory(ServiceDataFactory.class).to(ServiceData.class).in(RequestScoped.class);
  }

  @Override
  public void inject(ContainerRequestContext containerRequestContext, ServiceData serviceData) {
    containerRequestContext.setProperty(SERVICE_DATA_CONTEXT_KEY, serviceData);
  }

  public static class ServiceDataFactory implements Supplier<ServiceData> {

    private final ContainerRequestContext containerRequestContext;

    @Inject
    public ServiceDataFactory(ContainerRequestContext containerRequestContext) {
      this.containerRequestContext = containerRequestContext;
    }

    @Override
    public ServiceData get() {
      return (ServiceData) containerRequestContext.getProperty(SERVICE_DATA_CONTEXT_KEY);
    }
  }
}
