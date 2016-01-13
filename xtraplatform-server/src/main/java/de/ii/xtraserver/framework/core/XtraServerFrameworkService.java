/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraserver.framework.core;


public class XtraServerFrameworkService {//extends Application<XtraServerFrameworkConfiguration> {
/*
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(XtraServerFrameworkService.class);
    private static String version = XtraServerFrameworkService.class.getPackage().getImplementationVersion();
    private OsgiBundle osgi;
    private File cfgDir;
    private XtraServerFrameworkCommand cmd;

    private static XSFWindowsService xsfws;

    public static void serviceMain(String[] args, XSFWindowsService xsfwsin) throws Exception {
        xsfws = xsfwsin;
        new XtraServerFrameworkService(args).myRun(args);
    }

    public static void main(String[] args) throws Exception {
        new XtraServerFrameworkService(args).myRun(args);
    }

    public XtraServerFrameworkService(String[] args) {
        cfgDir = new File(args[1]).getParentFile();
    }

    public final void myRun(String[] arguments) throws Exception {
        final Bootstrap<XtraServerFrameworkConfiguration> bootstrap = new Bootstrap<>(this);
        cmd = new XtraServerFrameworkCommand<>(this);
        bootstrap.addCommand(cmd);
        initialize(bootstrap);
        final Cli cli = new Cli(new JarLocation(getClass()), bootstrap, System.out, System.err);
        if (!cli.run(arguments)) {
            // only exit if there's an error running the command
            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<XtraServerFrameworkConfiguration> bootstrap) {

        if (version == null || version.equals("unspecified")) {
            version = "snapshot";
        }

        bootstrap.addBundle(new ViewBundle());

        osgi = new OsgiBundle(cfgDir, "/*");
        // OSGi deactivated
        bootstrap.addBundle(osgi);

    }

    @Override
    public void run(XtraServerFrameworkConfiguration configuration, Environment environment) throws Exception {

        // TODO: get this from args ...
        XSFLogger.setLocale(Locale.ROOT);

        // set the Logging context always to root
        org.slf4j.MDC.put("orgid", "root");

        environment.jersey().setUrlPattern("/rest/*");

        environment.jersey().register(new FakeResource());

        environment.healthChecks().register("ModulesHealthCheck", new ModulesHealthCheck());

        environment.jersey().getResourceConfig().getContainerRequestFilters().add(QueryParamConnegFilter.class);
        environment.jersey().enable(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH);

        // TODO: enable trailing slashes, #36
        //environment.jersey().enable(ResourceConfig.FEATURE_REDIRECT);
        StopTask stopTask = new StopTask();
        environment.admin().addTask(stopTask);
        environment.lifecycle().addServerLifecycleListener(stopTask);

        // only on Windows as service
        if (xsfws != null) {
            environment.lifecycle().addServerLifecycleListener(xsfws);
        }

        environment.lifecycle().manage(osgi);

        final Server server = configuration.getServerFactory().build(environment);
        cmd.setServer(server);

        osgi.waitForLoggingAndSessions(20);

        LOGGER.info(FREE_STRING, "-----------------------------------------------------");
        LOGGER.info(FrameworkMessages.STARTING_XTRASERVER_FRAMEWORK_VERSION, version);
        LOGGER.debug(FrameworkMessages.INITIALIZING_MODULES);

        LOGGER.debug(FrameworkMessages.CONFIGURATION_DIRECTORY, cfgDir.getAbsolutePath());

        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (configuration.useFormattedJsonOutput) {
            environment.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            //LOGGER.warn(FrameworkMessages.GLOBALLY_ENABLED_JSON_PRETTY_PRINTING);
        }
    }*/
}
