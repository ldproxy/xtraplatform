package de.ii.xsf.dropwizard.api;

import org.apache.http.client.HttpClient;

/**
 *
 * @author zahnen
 */
public interface HttpClients {
    public HttpClient getDefaultHttpClient();
    public HttpClient getUntrustedSslHttpClient(String id);
    
}
