package de.ii.xtraplatform.akka.http;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import org.osgi.framework.BundleContext;

/**
 * @author zahnen
 */
public interface ActorSystemProvider {
    ActorSystem getActorSystem(BundleContext context, Config config);
}
