/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.akka.http;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.settings.ClientConnectionSettings;
import akka.http.javadsl.settings.ConnectionPoolSettings;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.util.Try;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class AkkaHttp implements de.ii.xtraplatform.akka.http.Http {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaHttp.class);

    public static final Config AKKA_HTTP_CONFIG = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .put("akka.stdout-loglevel", "OFF")
            .put("akka.loglevel", "DEBUG")
            .put("akka.loggers", ImmutableList.of("akka.event.slf4j.Slf4jLogger"))
            .put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter")
            //.put("akka.log-config-on-start", true)
            .put("akka.http.host-connection-pool.max-connections", 4)
            .put("akka.http.host-connection-pool.max-open-requests", 64)
            .put("akka.http.host-connection-pool.min-connections", 0)
            .put("akka.http.host-connection-pool.idle-timeout", "infinite")
            .put("akka.http.host-connection-pool.pool-implementation", "new")
            .put("akka.http.client.connecting-timeout", "10s")
            .put("akka.http.client.idle-timeout", "30s")
            .put("akka.http.parsing.max-chunk-size", "16m")
            .put("akka.http.parsing.illegal-header-warnings", "off")
            .build());

    @Controller
    private boolean ready;

    private final ActorSystem actorSystem;
    private final ActorMaterializer materializer;
    private final Http http;
    private final HttpClient defaultClient;

    public AkkaHttp(@Context BundleContext bundleContext, @Requires ActorSystemProvider actorSystemProvider) {
        this.actorSystem = actorSystemProvider.getActorSystem(bundleContext, AKKA_HTTP_CONFIG);
        this.materializer = ActorMaterializer.create(actorSystem);
        this.http = Http.get(actorSystem);
        this.defaultClient = new HttpHostClientAkka(materializer, createDefaultConnectionPool(actorSystem, http), true);
    }

    @Validate
    void onStart() {
        try {
            this.http.setDefaultClientHttpsContext(createTrustAllHttpsConnectionContext());
        } catch (Throwable e) {
            //ignore
        }

        this.ready = true;
    }

    @Override
    public HttpClient getDefaultClient() {
        return defaultClient;
    }

    @Override
    public HttpClient getHostClient(URI host, int maxParallelRequests, int idleTimeout) {
        boolean isHttps = Objects.equals(host.getScheme(), "https");
        int port = host.getPort() > 0 ? host.getPort() : isHttps ? 443 : 80;
        ConnectHttp connectHttp = isHttps ? ConnectHttp.toHostHttps(host.getHost(), port) : ConnectHttp.toHost(host.getHost(), port);
        ClientConnectionSettings connectionSettings = ClientConnectionSettings.create(actorSystem)
                                                                              .withIdleTimeout(Duration.create(idleTimeout, "s"));
        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.create(actorSystem)
                                                                              .withMaxOpenRequests(maxParallelRequests * 2)//???
                                                                              .withMinConnections(0)//???
                                                                              .withMaxConnections(maxParallelRequests)
                                                                              .withConnectionSettings(connectionSettings);

        //TODO: does it work for http?
        Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool> pool = isHttps
                ? http.cachedHostConnectionPoolHttps(connectHttp, connectionPoolSettings, actorSystem.log())
                : http.cachedHostConnectionPool(connectHttp, connectionPoolSettings, actorSystem.log());

        return new HttpHostClientAkka(materializer, pool);

    }

    private static Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> createDefaultConnectionPool(
            ActorSystem actorSystem, Http http) {
        return http.superPool(createDefaultConnectionPoolSettings(actorSystem), actorSystem.log());
    }

    private static ConnectionPoolSettings createDefaultConnectionPoolSettings(ActorSystem actorSystem) {
        return ConnectionPoolSettings.create(actorSystem)
                                     .withMaxOpenRequests(256)
                                     .withMinConnections(0)
                                     .withMaxConnections(64);
    }

    private static HttpsConnectionContext createTrustAllHttpsConnectionContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = {new TrustAll()};
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        return ConnectionContext.https(sslContext);
    }

    private static class TrustAll implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
