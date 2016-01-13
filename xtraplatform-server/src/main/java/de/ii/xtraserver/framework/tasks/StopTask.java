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
package de.ii.xtraserver.framework.tasks;

import com.google.common.collect.ImmutableMultimap;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraserver.framework.i18n.FrameworkMessages;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import org.eclipse.jetty.server.Server;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class StopTask extends Task implements ServerLifecycleListener {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(StopTask.class);
    private Server server = null;

    public StopTask() {
        super("stop");
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        if (server != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        LOGGER.info(FrameworkMessages.SHUTTING_DOWN_XTRASERVERFRAMEWORK);
                        server.stop();
                        LOGGER.info(FrameworkMessages.XTRASERVERFRAMEWORK_HAS_STOPPED);
                    } catch (Exception ex) {
                        LOGGER.error(FrameworkMessages.ERROR_WHEN_STOPPING_XTRASERVERFRAMEWORK, ex.getMessage() , ex);
                    }
                }
            }.start();
        }
    }

    @Override
    public void serverStarted(Server server) {
        this.server = server;
    }
}
