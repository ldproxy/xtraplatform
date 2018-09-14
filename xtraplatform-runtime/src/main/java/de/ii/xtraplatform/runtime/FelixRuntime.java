/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;

/**
 *
 * @author zahnen
 */
public class FelixRuntime {

    //private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FelixRuntime.class);
    
    public static final String DATA_DIR_KEY = "de.ii.xtraplatform.directories.data";
    private static final String DATA_DIR_NAME = "data";
    private static final String BUNDLES_DIR_NAME = "bundles";
    private static final String RUNTIME_BUNDLES_DIR_NAME = "runtime";
    private static final String PLATFORM_BUNDLES_DIR_NAME = "platform";
    private static final String FELIX_CACHE_DIR_NAME = "felix-cache";
    
    private static final Map<String, String> exports = new ImmutableMap.Builder<String, String>()
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
            .build();

    private final Map<String, String> felixConfig;
    private final File dataDir;
    private final File bundlesDir;
    private Felix felix;
    
    public static void main(String[] args) throws Exception {
        File dataDir;
        File bundlesDir;

        if (args.length >= 1) {
            dataDir = new File(args[0]);
        } else {
            dataDir = new File(DATA_DIR_NAME).getAbsoluteFile();
            if (!dataDir.exists()) {
                dataDir = new File("../" + DATA_DIR_NAME).getAbsoluteFile();
            }
        }
        if (!dataDir.exists()) {
            System.out.println("ERROR: NO DATA DIR FOUND");
            return;    
        }
        System.out.println("DATA DIR: " + dataDir.getAbsolutePath());

        if (args.length >= 2) {
            bundlesDir = new File(args[1]);
        } else {
            bundlesDir = new File(BUNDLES_DIR_NAME).getAbsoluteFile();
            if (!bundlesDir.exists()) {
                bundlesDir = new File("../" + BUNDLES_DIR_NAME).getAbsoluteFile();
            }
        }
        if (!bundlesDir.exists()) {
            System.out.println("ERROR: NO BUNDLES DIR FOUND");
            return;
        }
        System.out.println("BUNDLES DIR: " + bundlesDir.getAbsolutePath());

        FelixRuntime fr = new FelixRuntime(dataDir, bundlesDir);
        fr.init();
        fr.start();
    }

    public FelixRuntime(File dataDir, File bundlesDir) {
        this.felixConfig = new HashMap<>();
        this.dataDir = dataDir;
        this.bundlesDir = bundlesDir;
    }

    public void init() throws Exception {
        
        File runtimeBundleDir = new File(bundlesDir, RUNTIME_BUNDLES_DIR_NAME);
        String levelOne = "";
        String levelTwo = "";
        for (File f : runtimeBundleDir.listFiles()) {
            if (f.getName().endsWith(".jar")) {
                if (!f.getName().startsWith("org.apache.felix.http")) {
                    levelOne += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                } else {
                    levelTwo += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                }
            }
        }

        File platformBundleDir = new File(bundlesDir, PLATFORM_BUNDLES_DIR_NAME);
        String levelThree = "";
        String levelFour = "";
        for (File f : platformBundleDir.listFiles()) {
            if (f.getName().endsWith(".jar")) {
                if (f.getName().startsWith("ldproxy-wfs3-geojson") || f.getName().startsWith("ldproxy-wfs3-html") || f.getName().startsWith("ldproxy-wfs3-jsonld") || f.getName().startsWith("xtraplatform-feature-provider-pgis") || f.getName().startsWith("xtraplatform-feature-query-wfs")) {
                    levelThree += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                } else {
                    levelFour += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                }
            }
        }

        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".1", levelOne);
        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".2", levelTwo);
        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".3", levelThree);
        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".4", levelFour);
        felixConfig.put(FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "5");
        
        //felixConfig.put("felix.fileinstall.dir", new File(bundlesDir, PLATFORM_BUNDLES_DIR_NAME).getAbsolutePath());
        //felixConfig.put("felix.fileinstall.active.level", "1");
        //felixConfig.put("felix.fileinstall.start.level", "5");
        //felixConfig.put("felix.fileinstall.filter", "^.*");
        
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_ACTION_PROPERY, "install,start");
        
        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE, new File(dataDir, FELIX_CACHE_DIR_NAME).getAbsolutePath());
        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE_CLEAN, FelixConstants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        // Export the host provided service interface package.
        felixConfig.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Joiner.on(',').withKeyValueSeparator(";version=").join(exports));
        felixConfig.put(FelixConstants.FRAMEWORK_BOOTDELEGATION, "sun.misc");

        // Create host activator;
        //List<BundleActivator> list = new ArrayList();
        //list.add(this);
        //felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        
        felixConfig.put(DATA_DIR_KEY, dataDir.getAbsolutePath());
        
        try {
            // Now create an instance of the framework with
            // our configuration properties.
            felix = new Felix(felixConfig);
            felix.init();
        } catch (Exception ex) {
            //LOGGER.getLogger().error("Could not create Felix OSGi runtime: " + ex);
        }
        
        // (9) Use the system bundle context to process the auto-deploy
        // and auto-install/auto-start properties.
        AutoProcessor.process(felixConfig, felix.getBundleContext());

    }

    public void start() {

        try {
            
            // Now start Felix instance.
            felix.start();
                        
            System.out.println("FELIX_STARTED");
            //LOGGER.getLogger().info("FELIX_STARTED");
        } catch (Exception ex) {
            //LOGGER.getLogger().error("Could not create Felix OSGi runtime: " + ex);
        }

    }

    public void stop() throws Exception {
        felix.stop();
        felix.waitForStop(0);
    }
}
