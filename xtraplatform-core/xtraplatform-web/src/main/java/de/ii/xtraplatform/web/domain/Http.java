package de.ii.xtraplatform.web.domain;

import java.net.URI;

public interface Http {
  HttpClient getDefaultClient();

  HttpClient getHostClient(URI host, int maxParallelRequests, int idleTimeout);
}
