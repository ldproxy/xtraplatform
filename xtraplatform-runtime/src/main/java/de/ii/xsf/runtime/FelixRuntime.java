package de.ii.xsf.runtime;

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
    
    public static final String CFG_DIR_KEY = "de.ii.xsf.dirs.cfg";
    
    private static final Map<String, String> exports = new ImmutableMap.Builder<String, String>()
            //.put("javax.xml.bind", "0.0")
            //.put("javax.mail.internet", "0.0")
            .put("javax.management", "0.0")
            .put("javax.naming", "0.0")
            .put("javax.net.ssl", "0.0")
            .put("javax.net", "0.0")
            .put("sun.misc", "0.0")
            .put("sun.reflect", "0.0")
//            .put("org.json", "20070829")
//            .put("org.apache.commons.beanutils", "1.8.3")
// TODO
            .put("org.apache.felix.main", "0.0")
            .put("org.apache.felix.framework", "0.0")
            .build();

    private Map felixConfig = new HashMap();
    private File cfgDir;
    
    private Felix felix = null;
    
    public static void main(String[] args) throws Exception {
        File cfgDir;

        if (args.length >= 1) {
            cfgDir = new File(args[1]).getParentFile();
        } else {
            cfgDir = new File("cfg").getAbsoluteFile();
            if (!cfgDir.exists()) {
                cfgDir = new File("../cfg").getAbsoluteFile();
            }
        }
        if (!cfgDir.exists()) {
            System.out.println("ERROR: NO CFG DIR FOUND");
            return;    
        }
        System.out.println("CFG DIR: " + cfgDir.getAbsolutePath());

        FelixRuntime fr = new FelixRuntime(cfgDir);
        fr.init();
        fr.start();
    }

    public FelixRuntime(File cfgDir) {
        this.cfgDir = cfgDir;
    }

    public void init() throws Exception {
        
        String bundleDir = new File(new File(cfgDir.getParentFile(), "bundles"), "runtime").getAbsolutePath();
        String levelOne = "";
        String levelTwo = "";
        for (File f : new File(bundleDir).listFiles()) {
            if (f.getName().endsWith(".jar")) {
                if (!f.getName().startsWith("org.apache.felix.http")) {
                    levelOne += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                } else {
                    levelTwo += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                }
            }
        }

        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".1", levelOne);
        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".2", levelTwo);
        felixConfig.put(FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "3");
        
        felixConfig.put("felix.fileinstall.dir", new File(new File(cfgDir.getParentFile(), "bundles"), "platform").getAbsolutePath());
        felixConfig.put("felix.fileinstall.active.level", "1");
        felixConfig.put("felix.fileinstall.start.level", "3");
        //felixConfig.put("felix.fileinstall.filter", "^xsf-.*");
        felixConfig.put("felix.fileinstall.filter", "^.*");
        
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_ACTION_PROPERY, "install,start");
        
        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE, new File(cfgDir, "felix-cache").getAbsolutePath());

        // Export the host provided service interface package.
        felixConfig.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Joiner.on(',').withKeyValueSeparator(";version=").join(exports));

        // Create host activator;
        //List<BundleActivator> list = new ArrayList();
        //list.add(this);
        //felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        
        felixConfig.put(CFG_DIR_KEY, cfgDir.getAbsolutePath());
        
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
