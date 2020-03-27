package de.ii.xtraplatform.akka.http;

import akka.actor.ActorSystem;
import akka.http.javadsl.ClientTransport;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.headers.HttpCredentials;
import akka.http.javadsl.settings.ClientConnectionSettings;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static de.ii.xtraplatform.akka.http.AkkaHttp.createRegexFromGlob;

public class HttpProxyTransportGeneric extends ClientTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxyTransportGeneric.class);

    private final List<String> nonProxyHosts;
    private final ClientTransport delegateDirect = ClientTransport.TCP();
    private final ClientTransport delegateHttpProxy;
    private final ClientTransport delegateHttpsProxy;

    public HttpProxyTransportGeneric(URI proxyUri, List<String> nonProxyHosts) {
        this(proxyUri, nonProxyHosts, null, null);
    }

    public HttpProxyTransportGeneric(URI proxyUri, List<String> nonProxyHosts, String username, String password) {
        this.nonProxyHosts = nonProxyHosts;

        InetSocketAddress proxyAddress = InetSocketAddress.createUnresolved(proxyUri.getHost(), proxyUri.getPort());
        Optional<HttpCredentials> proxyCredentials = Optional.ofNullable(username).map(user -> HttpCredentials.createBasicHttpCredentials(user, password));

        if (proxyCredentials.isPresent()) {
            this.delegateHttpProxy = new HttpProxyTransport(proxyAddress, proxyCredentials.get());
            this.delegateHttpsProxy = ClientTransport.httpsProxy(proxyAddress, proxyCredentials.get());
        } else {
            this.delegateHttpProxy = new HttpProxyTransport(proxyAddress);
            this.delegateHttpsProxy = ClientTransport.httpsProxy(proxyAddress);
        }
    }

    @Override
    public Flow<ByteString, ByteString, CompletionStage<OutgoingConnection>> connectTo(String host,
                                                                                       int port,
                                                                                       ClientConnectionSettings settings,
                                                                                       ActorSystem system) {
        if (useProxyForHost(host)) {
            if (port == 443) {
                return delegateHttpsProxy.connectTo(host, port, settings, system);
            }

            return delegateHttpProxy.connectTo(host, port, settings, system);
        }

        return delegateDirect.connectTo(host, port, settings, system);
    }

    private boolean useProxyForHost(String host) {
        if (nonProxyHosts.stream()
                         .noneMatch(glob -> host.matches(createRegexFromGlob(glob)))) {
            String resolved = null;
            try {
                resolved = InetAddress.getByName(host)
                                      .getHostAddress();
            } catch (UnknownHostException e) {
                //ignore
            }
            String ip = Optional.ofNullable(resolved)
                                .orElse(host);

            if (host.equals(ip) || nonProxyHosts.stream()
                                                .noneMatch(glob -> ip.matches(createRegexFromGlob(glob)))) {
                return true;
            }
        }

        return false;
    }
}
