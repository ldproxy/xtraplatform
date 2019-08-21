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
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpCharsets;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.headers.AcceptEncoding;
import akka.http.javadsl.settings.ConnectionPoolSettings;
import akka.http.scaladsl.model.headers.HttpEncodings;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
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
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {AkkaHttp.class})
@Instantiate
public class AkkaHttp {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaHttp.class);

    public static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .put("akka.stdout-loglevel", "OFF")
            .put("akka.loglevel", "DEBUG")
            .put("akka.loggers", ImmutableList.of("akka.event.slf4j.Slf4jLogger"))
            .put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter")
            //.put("akka.log-config-on-start", true)
            .put("akka.http.host-connection-pool.max-connections", 4)
            .put("akka.http.host-connection-pool.max-open-requests", 64)
            .put("akka.http.host-connection-pool.min-connections", 1)
            .put("akka.http.host-connection-pool.idle-timeout", "infinite")
            .put("akka.http.host-connection-pool.pool-implementation", "new")
            .put("akka.http.client.connecting-timeout", "10s")
            .put("akka.http.client.idle-timeout", "30s")
            .put("akka.http.parsing.max-chunk-size", "16m")
            .put("akka.http.parsing.illegal-header-warnings", "off")
            .build());

    private static final Function<HttpResponse, HttpResponse> decodeResponse = response -> {
        // Pick the right coder
        final Coder coder;
        if (HttpEncodings.gzip()
                         .equals(response.encoding())) {
            coder = Coder.Gzip;
        } else if (HttpEncodings.deflate()
                                .equals(response.encoding())) {
            coder = Coder.Deflate;
        } else {
            coder = Coder.NoCoding;
        }
        LOGGER.debug("HTTP Encoding {}", coder);

        // Decode the entity
        return coder.decodeMessage(response);
    };

    @Context
    private BundleContext bundleContext;

    @Requires
    private ActorSystemProvider actorSystemProvider;

    @Controller
    private boolean ready;

    private ActorSystem actorSystem;

    private Http http;
    private ActorMaterializer materializer;
    private Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> pool;
    private final Map<String, Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool>> pools;

    public AkkaHttp() {
        this.pools = new HashMap<>();
    }

    @Validate
    void onStart() {
        this.actorSystem = actorSystemProvider.getActorSystem(bundleContext, config);

        if (Objects.isNull(actorSystem)) {
            throw new IllegalStateException("ActorSystem could not be acquired");
        }

        this.http = Http.get(actorSystem);

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = {new TrustAll()};
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            this.http.setDefaultClientHttpsContext(ConnectionContext.https(sslContext));
            /*this.http.setDefaultClientHttpsContext(http.createClientHttpsContext(AkkaSSLConfig.get(actorSystem)
                                                                                              .convertSettings(s -> s.withLoose(s.loose()
                                                                                                                                 .withAcceptAnyCertificate(true)
                                                                                                                                 .withDisableHostnameVerification(true)
                                                                                                                                 .withDisableSNI(true)))));
*/
        } catch (Throwable e) {
            //ignore
        }

        this.materializer = ActorMaterializer.create(actorSystem);
        this.pool = http.superPool(ConnectionPoolSettings.create(actorSystem)
                                                         .withMaxOpenRequests(256)
                                                         .withIdleTimeout(Duration.create(30, "s"))
                                                         .withMinConnections(1)
                                                         .withMaxConnections(64), actorSystem.log());

        this.ready = true;
    }

    @Invalidate
    void onStop() {
        if (actorSystemProvider != null && actorSystem != null) {
            //actorSystemProvider.stopActorSystem(actorSystem);
        }
    }

    public void registerHost(URI host) {
        String identifier = host.getScheme() + "://" + host.getHost();
        int port = host.getPort() > 0 ? host.getPort() : 80;
        if (port != 80) {
            identifier += ":" + port;
        }

        if (!pools.containsKey(identifier)) {
            ConnectHttp connectHttp = host.getScheme().equals("https") ? ConnectHttp.toHostHttps(host.getHost(), port) : ConnectHttp.toHost(host.getHost(), port);
            ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.create(actorSystem)
                                                             .withMaxOpenRequests(64)
                                                             .withMinConnections(1)
                                                             .withMaxConnections(4);

            Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool> pool = host.getScheme().equals("https")
                    ? http.cachedHostConnectionPoolHttps(connectHttp)
                    : http.cachedHostConnectionPool(connectHttp);

            this.pools.put(identifier, pool);
        }
    }

    private Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool> getPool(String url) {
        String identifier = url.substring(0, url.indexOf("/", 8));

        return pools.get(identifier);
    }

    public ActorMaterializer getMaterializer() {
        return materializer;
    }

    public CompletionStage<HttpResponse> getResponse(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }

    public CompletionStage<HttpResponse> getResponse(HttpRequest httpRequest) {
        return http.singleRequest(httpRequest);
    }

    public InputStream getAsInputStream(String url) {
        return get(url).runWith(StreamConverters.asInputStream(), materializer);
    }

    public String getAsString(String url) {
        StringBuilder response = new StringBuilder();
        Source<ByteString, NotUsed> source = get(url);

        source.runWith(Sink.fold(response, (Function2<StringBuilder, ByteString, StringBuilder>) (stringBuilder, byteString) -> stringBuilder.append(byteString.utf8String())), materializer)
              .toCompletableFuture()
              .join();

        return response.toString();
    }

    public Source<ByteString, NotUsed> get(String url) {

        LOGGER.debug("HTTP GET {}", url);
        // TODO: measure performance with files to compare processing time only
//        Source<ByteString, Date> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count.get() + "-" + page.get() + ".xml"))
//                .mapMaterializedValue(nu -> new Date());

        return Source.single(Pair.create(HttpRequest.create(url)
                                                    .addHeader(AcceptEncoding.create(HttpEncodings.deflate()
                                                                                                  .toRange(), HttpEncodings.gzip()
                                                                                                                           .toRange(), HttpEncodings.chunked()
                                                                                                                                                    .toRange())), null))
                     .via(getPool(url))
                     .map(param -> {
                         //LOGGER.debug("HTTP RESPONSE {}", param.toString());

                         if (param.first()
                                  .isFailure()) {
                             throw param.first()
                                        .failed()
                                        .getOrElse(() -> new IllegalStateException("Unknown HTTP client error"));
                         }
                         return param.first()
                                     .get();
                     })
                     .map(decodeResponse::apply)
                     //.mapMaterializedValue(nu -> new Date())
                     .flatMapConcat(httpResponse -> {
                         LOGGER.debug("HTTP RESPONSE {}", httpResponse.status());
                         return httpResponse.entity()
                                            .withoutSizeLimit()
                                            .getDataBytes();
                     });


        //return queryEncoder.encode(query)
        //                   .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    public Source<ByteString, NotUsed> postXml(String url, String body) {
        return post(url, body, MediaTypes.APPLICATION_XML.toContentType(HttpCharsets.UTF_8));
    }

    public Source<ByteString, NotUsed> post(String url, String body, ContentType.NonBinary contentType) {

        LOGGER.debug("HTTP POST {}\n{}", url, body);
        // TODO: measure performance with files to compare processing time only
//        Source<ByteString, Date> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count.get() + "-" + page.get() + ".xml"))
//                .mapMaterializedValue(nu -> new Date());

        return Source.single(Pair.create(HttpRequest.POST(url)
                                                    .withEntity(contentType, body)
                                                    .addHeader(AcceptEncoding.create(HttpEncodings.deflate()
                                                                                                  .toRange(), HttpEncodings.gzip()
                                                                                                                           .toRange(), HttpEncodings.chunked()
                                                                                                                                                    .toRange())), null))
                     .via(getPool(url))
                     .map(param -> {
                         //LOGGER.debug("HTTP RESPONSE {}", param.toString());
                         return param.first()
                                     .get();
                     })
                     .map(decodeResponse::apply)
                     //.mapMaterializedValue(nu -> new Date())
                     .flatMapConcat(httpResponse -> {
                         LOGGER.debug("HTTP RESPONSE {}", httpResponse.status());
                         return httpResponse.entity()
                                            .withoutSizeLimit()
                                            .getDataBytes();
                     });


        //return queryEncoder.encode(query)
        //                   .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    static class TrustAll implements X509TrustManager {

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
