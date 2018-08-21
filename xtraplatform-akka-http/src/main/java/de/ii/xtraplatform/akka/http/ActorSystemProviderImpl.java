/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.akka.http;

import akka.actor.ActorSystem;
import akka.osgi.OsgiActorSystemFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class ActorSystemProviderImpl implements ActorSystemProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSystemProviderImpl.class);

    @Override
    public ActorSystem getActorSystem(final BundleContext context, final Config config) {
        LOGGER.debug("AKKA STARTING");
        try {
            final ActorSystem system = new OsgiActorSystemFactory(context, scala.Option.empty(), ConfigFactory.load(config)).createActorSystem(scala.Option.empty());
            LOGGER.debug("AKKA STARTED");
            return system;
        } catch (Throwable e) {
            LOGGER.debug("AKKA START FAILED", e);
        }
        return null;
    }

}
