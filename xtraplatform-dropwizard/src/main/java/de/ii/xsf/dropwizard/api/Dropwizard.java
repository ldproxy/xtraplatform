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
package de.ii.xsf.dropwizard.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import de.ii.xsf.dropwizard.cfg.XtraServerFrameworkConfiguration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
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
}
