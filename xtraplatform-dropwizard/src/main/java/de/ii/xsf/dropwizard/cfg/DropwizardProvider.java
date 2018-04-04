/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.cfg;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.dropwizard.api.HttpClients;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.Cli;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.JarLocation;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import org.apache.felix.ipojo.annotations.*;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContextBuilder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.joda.time.DateTime;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class DropwizardProvider extends Application<XtraServerFrameworkConfiguration> implements Dropwizard, HttpClients {

    private static final Logger ROOT_LOGGER = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DropwizardProvider.class);
    public static final String CFG_FILE_NAME = "xtraplatform.json";
    public static final String CFG_FILE_TEMPLATE_NAME = "/xtraplatform.default.json";
    public static final String TEMPLATE_DIR_NAME = "templates";
    public static final String DW_CMD = "server";

    // Service not published by default
    @ServiceController(value = false)
    private boolean controller;

    @Context
    private BundleContext context;

    private XtraServerFrameworkConfiguration configuration;
    private Environment environment;
    private ServletContainer jerseyContainer;
    private ViewRenderer mustacheRenderer;

    public DropwizardProvider() {
    }

    @Validate
    public void start() {

        // TODO: move to config store
        File cfgFile = new File(new File(context.getProperty(DATA_DIR_KEY)), CFG_FILE_NAME);

        try {
            if (!cfgFile.isFile()) {
                Resources.asByteSource(Resources.getResource(DropwizardProvider.class, CFG_FILE_TEMPLATE_NAME)).copyTo(new FileOutputStream(cfgFile));
            }

            init(cfgFile);

            // publish the service once the initialization
            // is completed.
            controller = true;

            LOGGER.debug("Initialized Dropwizard with configuration file {}", cfgFile.getAbsolutePath());

        } catch (Exception ex) {
            LOGGER.error("Error initializing Dropwizard with configuration file {}", cfgFile.getAbsolutePath(), ex);
        }
    }

    public void init(File cfgFile) throws Exception {
        final Bootstrap<XtraServerFrameworkConfiguration> bootstrap = new Bootstrap<>(this);
        bootstrap.addCommand(new XtraServerFrameworkCommand<>(this));
        initialize(bootstrap);

        final Cli cli = new Cli(new JarLocation(getClass()), bootstrap, System.out, System.err);
        String[] arguments = {DW_CMD, cfgFile.getAbsolutePath()};
        if (!cli.run(arguments)) {
            throw new Exception("CLI ERROR");
        }
    }

    @Override
    public void initialize(Bootstrap<XtraServerFrameworkConfiguration> bootstrap) {
        this.mustacheRenderer = new FallbackMustacheViewRenderer();

        bootstrap.addBundle(new ViewBundle<XtraServerFrameworkConfiguration>(ImmutableSet.of(mustacheRenderer)) {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(XtraServerFrameworkConfiguration configuration) {
                return ImmutableMap.of(".mustache", ImmutableMap.of("fileRoot", new File(new File(context.getProperty(DATA_DIR_KEY)), TEMPLATE_DIR_NAME).getAbsolutePath()));
            }
        });
    }

    @Override
    public void run(XtraServerFrameworkConfiguration configuration, Environment environment) throws Exception {
        this.configuration = configuration;
        this.environment = environment;
        this.jerseyContainer = (ServletContainer) environment.getJerseyServletContainer();

        this.environment.healthChecks().register("ModulesHealthCheck", new ModulesHealthCheck());

        // TODO: enable trailing slashes, #36
        //environment.jersey().enable(ResourceConfig.FEATURE_REDIRECT);
        this.environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (configuration.useFormattedJsonOutput) {
            environment.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            //LOGGER.warn(FrameworkMessages.GLOBALLY_ENABLED_JSON_PRETTY_PRINTING);
        }
    }

    @Override
    public XtraServerFrameworkConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    // TODO: use connection manager

    @Override
    public HttpClient getDefaultHttpClient() {
        return getHttpClient("xsf", null);
    }

    // TODO: is available now via configuration: http://www.dropwizard.io/1.2.0/docs/manual/configuration.html#man-configuration-clients-http
    @Override
    public HttpClient getUntrustedSslHttpClient(String id) {
        Registry<ConnectionSocketFactory> registry = null;

        try {
            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new TrustSelfSignedStrategy())
                    .build();

            // we can optionally disable hostname verification.
            // if you don't want to further weaken the security, you don't have to include this.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

            // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
            // and allow all hosts verifier.
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

            registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
            //ignore
        }

        return getHttpClient(id, registry);
    }

    private HttpClient getHttpClient(String id, Registry<ConnectionSocketFactory> registry) {

        HttpClientBuilder hcb = new HttpClientBuilder(getEnvironment()).using(getConfiguration().httpClient);

        if (registry != null) {
            hcb = hcb.using(registry);
        }

        CloseableHttpClient httpclient = hcb.build(id + new DateTime().toString());

        // TODO: not needed anymore in 4.5, verify
        /*httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

            @Override
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip, deflate");
                }
            }

        });

        httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

            @Override
            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null) {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                response.setEntity(
                                        new GzipDecompressingEntity(response.getEntity()));
                                return;
                            } else if (codecs[i].getName().equalsIgnoreCase("deflate")) {
                                response.setEntity(
                                        new DeflateDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });*/

        return httpclient;
    }

    @Override
    public Map<String, Boolean> getFlags() {
        Map<String, Boolean> flags = new HashMap<>();

        flags.put(FLAG_ALLOW_SERVICE_READDING, getConfiguration().allowServiceReAdding);
        flags.put(FLAG_USE_FORMATTED_JSON_OUTPUT, getConfiguration().useFormattedJsonOutput);

        return flags;
    }

    @Override
    public ServletEnvironment getServlets() {
        return getEnvironment().servlets();
    }

    @Override
    public ServletContext getServletContext() {
        return getEnvironment().getApplicationContext().getServletContext();
    }

    @Override
    public MutableServletContextHandler getApplicationContext() {
        return getEnvironment().getApplicationContext();
    }

    @Override
    public JerseyEnvironment getJersey() {
        return getEnvironment().jersey();
    }

    @Override
    public ServletContainer getJerseyContainer() {
        return jerseyContainer;
    }

    @Override
    public String getExternalUrl() {
        if (!hasExternalUrl()) {
                    return "http://" + getHostName() + ":" + String.valueOf(getApplicationPort()) + "/";
        }

        return getConfiguration().externalURL.endsWith("/") ? getConfiguration().externalURL : getConfiguration().externalURL + "/";
    }

    @Override
    public boolean hasExternalUrl() {
        return getConfiguration().externalURL != null && !getConfiguration().externalURL.isEmpty();
    }

    @Override
    public int getApplicationPort() {
        return ((HttpConnectorFactory) ((DefaultServerFactory) getConfiguration().getServerFactory()).getApplicationConnectors().get(0)).getPort();
    }

    @Override
    public String getHostName() {
        String hostName = "";
        try {
            InetAddress iAddress = InetAddress.getLocalHost();
            hostName = iAddress.getCanonicalHostName();
            if (hostName == null || hostName.isEmpty()) {
                hostName = iAddress.getHostName();
            }
        } catch (UnknownHostException e) {
            // failed;  try alternate means.
        }
        // windows environment variable
        if (hostName == null || hostName.isEmpty()) {
            hostName = System.getenv("COMPUTERNAME");
        }
        // linux environment variable
        if (hostName == null || hostName.isEmpty()) {
            hostName = System.getenv("HOSTNAME");
        }
        // last option
        if (hostName == null || hostName.isEmpty()) {
            hostName = "localhost";
        }

        return hostName;
    }

    @Override
    public int getDebugLogMaxMinutes() {
        return getConfiguration().maxDebugLogDurationMinutes;
    }

    @Override
    public void attachLoggerAppender(Appender appender) {
        if (!appender.isStarted()) {
            appender.setContext(ROOT_LOGGER.getLoggerContext());
            appender.start();
        }
        ROOT_LOGGER.addAppender(appender);
    }

    @Override
    public void detachLoggerAppender(Appender appender) {
        ROOT_LOGGER.detachAppender(appender);
        appender.stop();
    }

    @Override
    public void setLoggingLevel(Level level) {
        // TODO: this only works for loggers whose level is not set explicitely in config
        ROOT_LOGGER.setLevel(level);
    }

    @Override
    public void resetServer() {
        // cleanup metrics
        for (String name : environment.metrics().getNames()) {
            if (name.contains("jetty")) {
                environment.metrics().remove(name);
            }
        }
        
        // cleanup jersey
        // still not sure why this is needed and why it works
        // if we don't do this, we get exceptions from the jersey thread local stack
        // that handles context injections for singleton resource classes
        // the jersey ServletContainer is a thin servlet wrapper around the resource config
        // we create a new instance of ServletContainer and replace the old one in the jetty config
        // BUT: if we destroy the old one and/or replace the reference that JaxRsRegistry uses for reloads,
        // we get exceptions again
        for (ServletHolder sh : getApplicationContext().getServletHandler().getServlets()) {
            if (sh.getName().contains("jersey")) {
                LOGGER.debug("JERSEY CLEANUP");
                ServletContainer sc = new ServletContainer(environment.jersey().getResourceConfig());
                sh.setServlet(sc);
                
                //this.jerseyContainer.reload();
                //this.jerseyContainer.destroy();
                //this.jerseyContainer = sc;                
            }
        }
    }

    @Override
    public ViewRenderer getMustacheRenderer() {
        return mustacheRenderer;
    }
}
