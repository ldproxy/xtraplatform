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
@Provides(specifications = {ActorSystemProvider.class})
@Instantiate
public class ActorSystemProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSystemProvider.class);

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
