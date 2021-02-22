/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import akka.actor.ActorSystem;
import akka.osgi.OsgiActorSystemFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Component
@Provides
@Instantiate
public class ActorSystemProviderImpl implements ActorSystemProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActorSystemProviderImpl.class);

  private static final Config DEFAULT_CONFIG =
      ConfigFactory.parseMap(
          new ImmutableMap.Builder<String, Object>()
              .put("akka.stdout-loglevel", "OFF")
              .put("akka.loglevel", "INFO")
              .put("akka.loggers", ImmutableList.of("akka.event.slf4j.Slf4jLogger"))
              .put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter")
              // .put("akka.log-config-on-start", true)
              .build());

  @Override
  public ActorSystem getActorSystem(BundleContext context) {
    return getActorSystem(context, DEFAULT_CONFIG);
  }

  @Override
  public ActorSystem getActorSystem(final BundleContext context, final Config config) {
    return getActorSystem(
        context,
        config,
        context
            .getBundle()
            .getSymbolicName()
            .substring(context.getBundle().getSymbolicName().lastIndexOf(".") + 1));
  }

  @Override
  public ActorSystem getActorSystem(BundleContext context, Config config, String name) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Starting Akka for bundle {} ...", context.getBundle().getSymbolicName());
    }
    try {
      final ActorSystem system =
          new OsgiActorSystemFactory(context, scala.Option.empty(), ConfigFactory.load(config))
              .createActorSystem(name);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("... Akka started");
      }

      return system;
    } catch (Throwable e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("AKKA START FAILED", e);
      }
    }
    return null;
  }
}
