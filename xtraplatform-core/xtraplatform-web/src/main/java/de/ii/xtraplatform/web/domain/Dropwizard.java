/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import javax.servlet.ServletContext;
import org.glassfish.jersey.servlet.ServletContainer;

/** @author zahnen */
//TODO: cleanup
public interface Dropwizard {

  ServletEnvironment getServlets();

  ServletContext getServletContext();

  MutableServletContextHandler getApplicationContext();

  JerseyEnvironment getJersey();

  ServletContainer getJerseyContainer();

  Environment getEnvironment();

  ViewRenderer getMustacheRenderer();
}
