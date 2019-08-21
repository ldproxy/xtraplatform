/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletContext;

/**
 *
 * @author zahnen
 */
public interface Dropwizard extends XtraPlatform {
    //public static final String FLAG_ALLOW_SERVICE_READDING = "allowServiceReAdding";
    //public static final String FLAG_USE_FORMATTED_JSON_OUTPUT = "useFormattedJsonOutput";
    
    //public Map<String,Boolean> getFlags();
    ServletEnvironment getServlets();
    ServletContext getServletContext();
    MutableServletContextHandler getApplicationContext();
    JerseyEnvironment getJersey();
    ServletContainer getJerseyContainer();
    //public int getDebugLogMaxMinutes();
    void attachLoggerAppender(Appender appender);
    void detachLoggerAppender(Appender appender);
    void setLoggingLevel(Level level);
    Environment getEnvironment();
    void resetServer();

    ViewRenderer getMustacheRenderer();
}
