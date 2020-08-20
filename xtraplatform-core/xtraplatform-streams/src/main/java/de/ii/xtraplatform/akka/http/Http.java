package de.ii.xtraplatform.akka.http;

import java.net.URI;

public interface Http {

    HttpClient getDefaultClient();

    HttpClient getHostClient(URI host, int maxParallelRequests, int idleTimeout);
}
