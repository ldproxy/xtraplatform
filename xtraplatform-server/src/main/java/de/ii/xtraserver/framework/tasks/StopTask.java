/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.tasks;

import com.google.common.collect.ImmutableMultimap;
import de.ii.xsf.logging.XSFLogger;
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
                        //LOGGER.info(FrameworkMessages.SHUTTING_DOWN_XTRASERVERFRAMEWORK);
                        server.stop();
                        //LOGGER.info(FrameworkMessages.XTRASERVERFRAMEWORK_HAS_STOPPED);
                    } catch (Exception ex) {
                        //LOGGER.error(FrameworkMessages.ERROR_WHEN_STOPPING_XTRASERVERFRAMEWORK, ex.getMessage() , ex);
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
