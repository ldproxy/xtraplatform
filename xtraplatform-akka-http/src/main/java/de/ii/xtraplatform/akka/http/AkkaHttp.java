/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.akka.http;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.Http;
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.AcceptEncoding;
import akka.http.javadsl.settings.ConnectionPoolSettings;
import akka.http.scaladsl.model.headers.HttpEncodings;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.sslconfig.akka.AkkaSSLConfig;
import com.typesafe.sslconfig.ssl.TrustManagerConfig;
import com.typesafe.sslconfig.ssl.TrustStoreConfig;
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
import scala.util.Try;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .put("akka.loglevel", "INFO")
            //.put("akka.log-config-on-start", true)
            .put("akka.http.host-connection-pool.max-connections", 32)
            .put("akka.http.host-connection-pool.pool-implementation", "new")
            .put("akka.http.parsing.max-chunk-size", "16m")
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
                                                         .withMaxConnections(64), actorSystem.log());

        this.ready = true;
    }

    @Invalidate
    void onStop() {
        if (actorSystemProvider != null && actorSystem != null) {
            //actorSystemProvider.stopActorSystem(actorSystem);
        }
    }

    public ActorMaterializer getMaterializer() {
        return materializer;
    }

    public CompletionStage<HttpResponse> getResponse(String url) {
        return http.singleRequest(HttpRequest.create(url));
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
                     .via(pool)
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
