package de.ii.xtraplatform.akka.http;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ClientTransport;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.headers.HttpCredentials;
import akka.http.javadsl.settings.ClientConnectionSettings;
import akka.japi.function.Function;
import akka.stream.javadsl.BidiFlow;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class HttpProxyTransport extends ClientTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxyTransport.class);

    private final InetSocketAddress proxyAddress;
    private final Optional<HttpCredentials> proxyCredentials;
    private final ClientTransport underlyingTransport = ClientTransport.TCP();
    private final boolean debug = true;

    public HttpProxyTransport(InetSocketAddress proxyAddress,
                              HttpCredentials proxyCredentials) {
        this.proxyAddress = proxyAddress;
        this.proxyCredentials = Optional.ofNullable(proxyCredentials);
    }

    public HttpProxyTransport(InetSocketAddress proxyAddress) {
        this(proxyAddress, null);
    }

    @Override
    public Flow<ByteString, ByteString, CompletionStage<OutgoingConnection>> connectTo(String host,
                                                                                       int port,
                                                                                       ClientConnectionSettings settings,
                                                                                       ActorSystem system) {

        Flow<ByteString, ByteString, NotUsed> identity = Flow.fromFunction((Function<ByteString, ByteString>) byteString -> byteString);
        Flow<ByteString, ByteString, CompletionStage<OutgoingConnection>> connect = underlyingTransport.connectTo(proxyAddress.getHostString(), proxyAddress.getPort(), settings, system);
        Function<CompletionStage<OutgoingConnection>, CompletionStage<OutgoingConnection>> rewriteOutgoingConnection = completionStage -> completionStage.thenApply(outgoingConnection -> new OutgoingConnection(new akka.http.scaladsl.Http.OutgoingConnection(outgoingConnection.localAddress(), InetSocketAddress.createUnresolved(host, port))));


        return BidiFlow.fromFlows(rewriteHeader(host, port), identity)
                       .join(connect, Keep.right())
                       .mapMaterializedValue(rewriteOutgoingConnection);
    }

    /**
     * Rewrites the http request meta to conform to the proxy format
     * <p>
     * before:
     * <p>
     * GET / HTTP/1.1
     * ...
     * <p>
     * after:
     * <p>
     * GET http://icanhazip.com:80/ HTTP/1.1
     * Proxy-Connection: Keep-Alive
     * Proxy-Authorization: Basic cGF1bGdob3N0OmNtODM1Ng==
     * ...
     */
    private Flow<ByteString, ByteString, ?> rewriteHeader(String host, int port) {

        return Flow.fromFunction(byteString -> {

            Tuple2<ByteString, ByteString> span = byteString.span(byt -> !Objects.equals(byt, "\n".getBytes()[0]));
            String withFullPath = span._1()
                                      .utf8String()
                                      .replace(" /", String.format(" http://%s:%d/", host, port));

            LOGGER.debug("PROXY: {}", withFullPath);

            return ByteString.fromString(withFullPath + span._2()
                                                            .utf8String());
        });
    }
}
