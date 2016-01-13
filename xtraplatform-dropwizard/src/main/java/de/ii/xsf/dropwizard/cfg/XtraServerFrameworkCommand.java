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
package de.ii.xsf.dropwizard.cfg;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
public class XtraServerFrameworkCommand<T extends Configuration> extends EnvironmentCommand<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);

    private final Class<T> configurationClass;
    private Server server;

    public XtraServerFrameworkCommand(Application<T> application) {
        super(application, "server", "Runs the Dropwizard application as an HTTP server");
        this.configurationClass = application.getConfigurationClass();
    }

    /*
     * Since we don't subclass ServerCommand, we need a concrete reference to the configuration
     * class.
     */
    @Override
    protected Class<T> getConfigurationClass() {
        return configurationClass;
    }

    @Override
    protected void run(Environment environment, Namespace namespace, T configuration) throws Exception {
        // this is needed if run is not blocking
        cleanupAsynchronously();
        
        /*System.out.println("COMMAND");
        if (server != null) {
                server.addLifeCycleListener(new LifeCycleListener());
                cleanupAsynchronously();
        }
        else {
            LOGGER.error("Server configuration missing, shutting down");
        }*/
    }

    private class LifeCycleListener extends AbstractLifeCycle.AbstractLifeCycleListener {

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            cleanup();
        }
    }

    public void setServer(Server server) {
        this.server = server;
    }

}
