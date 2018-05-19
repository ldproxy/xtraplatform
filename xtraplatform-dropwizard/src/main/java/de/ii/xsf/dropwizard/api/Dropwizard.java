/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import de.ii.xsf.dropwizard.cfg.XtraServerFrameworkConfiguration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Map;
import javax.servlet.ServletContext;

/**
 *
 * @author zahnen
 */
public interface Dropwizard {
    public static final String FLAG_ALLOW_SERVICE_READDING = "allowServiceReAdding";
    public static final String FLAG_USE_FORMATTED_JSON_OUTPUT = "useFormattedJsonOutput";
    
    public Map<String,Boolean> getFlags();
    public ServletEnvironment getServlets();
    public ServletContext getServletContext();
    public MutableServletContextHandler getApplicationContext();
    public JerseyEnvironment getJersey();
    public ServletContainer getJerseyContainer();
    public String getExternalUrl();
    public boolean hasExternalUrl();
    public int getApplicationPort();
    public String getHostName();
    public int getDebugLogMaxMinutes();
    public void attachLoggerAppender(Appender appender);
    public void detachLoggerAppender(Appender appender);
    public void setLoggingLevel(Level level);
    public XtraServerFrameworkConfiguration getConfiguration();
    public Environment getEnvironment();
    public void resetServer();

    ViewRenderer getMustacheRenderer();
}
