package de.ii.xtraplatform.osgi;


import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.DefaultLoggingFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FelixRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(FelixRuntime.class);

    public enum ENV {
        DEVELOPMENT,
        PRODUCTION,
        CONTAINER
    }

    public static final String DATA_DIR_KEY = "de.ii.xtraplatform.directories.data";
    public static final String ENV_KEY = "de.ii.xtraplatform.environment";

    private static final String ENV_VAR = "XTRAPLATFORM_ENV";
    private static final String DATA_DIR_NAME = "data";
    private static final String BUNDLES_DIR_NAME = "bundles";
    private static final String FELIX_CACHE_DIR_NAME = "felix-cache";
    private static final String CONFIG_FILE_NAME = "xtraplatform.json";

    private static final Map<String, String> EXPORTS = new ImmutableMap.Builder<String, String>()
            //.put("javax.xml.bind", "0.0")
            //.put("javax.mail.internet", "0.0")
            .put("javax.management", "0.0")
            .put("javax.naming", "0.0")
            .put("javax.net.ssl", "0.0")
            .put("javax.net", "0.0")
            .put("sun.misc", "0.0")
            .put("sun.reflect", "0.0")
            .put("sun.security.util", "0.0")
            .put("sun.security.x509", "0.0")
// TODO
            .put("org.apache.felix.main", "0.0")
            .put("org.apache.felix.framework", "0.0")

            .put("org.slf4j", "1.7.25")
            .put("org.slf4j.helpers", "1.7.25")
            .put("org.slf4j.event", "1.7.25")
            .put("org.slf4j.spi", "1.7.25")
            .put("org.slf4j.impl", "1.7.25")
            .put("ch.qos.logback.classic", "1.2.3")
            .put("ch.qos.logback.classic.spi", "1.2.3")
            .put("ch.qos.logback.classic.util", "1.2.3")
            .put("ch.qos.logback.classic.net", "1.2.3")
            .put("ch.qos.logback.classic.net.server", "1.2.3")
            .put("ch.qos.logback.classic.html", "1.2.3")
            .put("ch.qos.logback.classic.helpers", "1.2.3")
            .put("ch.qos.logback.classic.pattern.color", "1.2.3")
            .put("ch.qos.logback.classic.selector.servlet", "1.2.3")
            .put("ch.qos.logback.classic.joran", "1.2.3")
            .put("ch.qos.logback.classic.joran.action", "1.2.3")
            .put("ch.qos.logback.classic.db", "1.2.3")
            .put("ch.qos.logback.classic.db.names", "1.2.3")
            .put("ch.qos.logback.classic.log4j", "1.2.3")
            .put("ch.qos.logback.classic.jul", "1.2.3")
            .put("ch.qos.logback.classic.boolex", "1.2.3")
            .put("ch.qos.logback.classic.pattern", "1.2.3")
            .put("ch.qos.logback.classic.gaffer", "1.2.3")
            .put("ch.qos.logback.classic.filter", "1.2.3")
            .put("ch.qos.logback.classic.servlet", "1.2.3")
            .put("ch.qos.logback.classic.jmx", "1.2.3")
            .put("ch.qos.logback.classic.sift", "1.2.3")
            .put("ch.qos.logback.classic.selector", "1.2.3")
            .put("ch.qos.logback.classic.encoder", "1.2.3")
            .put("ch.qos.logback.classic.layout", "1.2.3")
            .put("ch.qos.logback.classic.turbo", "1.2.3")
            .put("ch.qos.logback.core", "1.2.3")
            .put("ch.qos.logback.core.sift", "1.2.3")
            .put("ch.qos.logback.core.read", "1.2.3")
            .put("ch.qos.logback.core.layout", "1.2.3")
            .put("ch.qos.logback.core.encoder", "1.2.3")
            .put("ch.qos.logback.core.net", "1.2.3")
            .put("ch.qos.logback.core.net.server", "1.2.3")
            .put("ch.qos.logback.core.net.ssl", "1.2.3")
            .put("ch.qos.logback.core.spi", "1.2.3")
            .put("ch.qos.logback.core.recovery", "1.2.3")
            .put("ch.qos.logback.core.helpers", "1.2.3")
            .put("ch.qos.logback.core.db", "1.2.3")
            .put("ch.qos.logback.core.db.dialect", "1.2.3")
            .put("ch.qos.logback.core.filter", "1.2.3")
            .put("ch.qos.logback.core.html", "1.2.3")
            .put("ch.qos.logback.core.boolex", "1.2.3")
            .put("ch.qos.logback.core.subst", "1.2.3")
            .put("ch.qos.logback.core.property", "1.2.3")
            .put("ch.qos.logback.core.util", "1.2.3")
            .put("ch.qos.logback.core.hook", "1.2.3")
            .put("ch.qos.logback.core.status", "1.2.3")
            .put("ch.qos.logback.core.pattern", "1.2.3")
            .put("ch.qos.logback.core.pattern.util", "1.2.3")
            .put("ch.qos.logback.core.pattern.color", "1.2.3")
            .put("ch.qos.logback.core.pattern.parser", "1.2.3")
            .put("ch.qos.logback.core.rolling", "1.2.3")
            .put("ch.qos.logback.core.rolling.helper", "1.2.3")
            .put("ch.qos.logback.core.joran", "1.2.3")
            .put("ch.qos.logback.core.joran.node", "1.2.3")
            .put("ch.qos.logback.core.joran.action", "1.2.3")
            .put("ch.qos.logback.core.joran.event", "1.2.3")
            .put("ch.qos.logback.core.joran.event.stax", "1.2.3")
            .put("ch.qos.logback.core.joran.util", "1.2.3")
            .put("ch.qos.logback.core.joran.util.beans", "1.2.3")
            .put("ch.qos.logback.core.joran.spi", "1.2.3")
            .put("ch.qos.logback.core.joran.conditional", "1.2.3")

            .build();

    private final String name;
    private final String version;
    private Felix felix;

    public FelixRuntime(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public void init(String[] args, List<List<String>> bundles) {

        File dataDir = getDataDir(args).orElseThrow(() -> new IllegalArgumentException("No data directory found"));
        File bundlesDir = getBundlesDir(args).orElseThrow(() -> new IllegalArgumentException("No bundles directory found"));

        preloadLoggingConfiguration(new File(dataDir, CONFIG_FILE_NAME));

        LOGGER.info("--------------------------------------------------");
        LOGGER.info("Starting {} {}", name, version);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Data directory: {}", dataDir);
            LOGGER.debug("Bundles directory: {}", bundlesDir);
        }

        Map<String, String> felixConfig = new HashMap<>();
        String bundlePrefix = "reference:file:" + bundlesDir.getAbsolutePath()
                .replaceAll(" ", "%20") + "/";
        int startLevel = 1;

        for (List<String> level : bundles) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Level: {}, Bundles: {}", startLevel, level);
            }

            String levelBundles = level.stream()
                    .map(bundle -> bundlePrefix + bundle)
                    .collect(Collectors.joining(" "));

            felixConfig.put(AutoProcessor.AUTO_START_PROP + "." + startLevel, levelBundles);

            startLevel++;
        }
        felixConfig.put(FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(startLevel));

        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE, new File(dataDir, FELIX_CACHE_DIR_NAME).getAbsolutePath());
        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE_CLEAN, FelixConstants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        // Export the host provided service interface package.
        felixConfig.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Joiner.on(',')
                .withKeyValueSeparator(";version=")
                .join(EXPORTS));
        felixConfig.put(FelixConstants.FRAMEWORK_BOOTDELEGATION, "sun.misc");

        felixConfig.put(DATA_DIR_KEY, dataDir.getAbsolutePath());
        felixConfig.put(ENV_KEY, Optional.ofNullable(System.getenv(ENV_VAR))
                .orElse(ENV.DEVELOPMENT.name()));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Felix config: {}", felixConfig);
        }

        try {
            this.felix = new Felix(felixConfig);
            felix.init();
        } catch (Exception ex) {
            LOGGER.error("Could not initialize felix: {}", ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("", ex);
            }
        }

        AutoProcessor.process(felixConfig, felix.getBundleContext());
    }

    public void start() {
        try {
            felix.start();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Felix started");
            }
        } catch (Exception ex) {
            LOGGER.error("Could not start felix: {}", ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("", ex);
            }
        }

    }

    public void stop(long timeout) {
        try {
            felix.stop();
            felix.waitForStop(timeout);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Felix stopped");
            }
        } catch (Exception ex) {
            LOGGER.error("Could not stop felix: {}", ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("", ex);
            }
        }
    }

    private Optional<File> getDataDir(String[] args) {
        File dataDir;

        if (args.length >= 1) {
            dataDir = new File(args[0]);
        } else {
            dataDir = new File(DATA_DIR_NAME).getAbsoluteFile();
            if (!dataDir.exists()) {
                dataDir = new File("../" + DATA_DIR_NAME).getAbsoluteFile();
            }
        }
        if (!dataDir.exists()) {
            return Optional.empty();
        }

        return Optional.of(dataDir);
    }

    private Optional<File> getBundlesDir(String[] args) {
        File bundlesDir;

        if (args.length >= 2) {
            bundlesDir = new File(args[1]);
        } else {
            bundlesDir = new File(BUNDLES_DIR_NAME).getAbsoluteFile();
            if (!bundlesDir.exists()) {
                bundlesDir = new File("../" + BUNDLES_DIR_NAME).getAbsoluteFile();
            }
        }
        if (!bundlesDir.exists()) {
            return Optional.empty();
        }

        return Optional.of(bundlesDir);
    }

    private void preloadLoggingConfiguration(File configFile) {

        DefaultLoggingFactory loggingFactory;

        try {
            ObjectMapper objectMapper = Jackson.newObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(configFile).at("/logging");
            loggingFactory = objectMapper.readerFor(DefaultLoggingFactory.class).readValue(jsonNode);
        } catch (Throwable e) {
            //ignore
            loggingFactory = new DefaultLoggingFactory();
        }

        loggingFactory.configure(new MetricRegistry(), "xtraplatform");
    }

}
