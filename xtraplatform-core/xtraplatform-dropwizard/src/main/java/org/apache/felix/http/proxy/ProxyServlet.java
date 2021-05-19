/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy;

import javax.servlet.ServletException;
import org.osgi.framework.BundleContext;

public final class ProxyServlet extends AbstractProxyServlet {
  private static final long serialVersionUID = -5740821969955405599L;

  protected BundleContext getBundleContext() throws ServletException {
    Object context = getServletContext().getAttribute(BundleContext.class.getName());
    if (context instanceof BundleContext) {
      return (BundleContext) context;
    }

    throw new ServletException(
        "Bundle context attribute ["
            + BundleContext.class.getName()
            + "] not set in servlet context");
  }
}
