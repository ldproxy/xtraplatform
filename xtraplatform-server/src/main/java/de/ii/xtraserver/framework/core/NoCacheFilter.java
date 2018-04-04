/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.core;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zahnen
 */
public class NoCacheFilter implements Filter {
  private FilterConfig filterConfig = null;
  
  public void init(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }
  
  public void destroy() {
    this.filterConfig = null;
  }

  /*The real work happens in doFilter(). 
  The reference to the response object is of type ServletResponse,
  so we need to cast it to HttpServletResponse:
  */

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    /*Then we just set the appropriate headers
    and invoke the next filter in the chain:
    */
    httpResponse.setHeader("Cache-Control", "no-cache");
    httpResponse.setDateHeader("Expires", 0);
    httpResponse.setHeader("Pragma", "No-cache");
    chain.doFilter(request, response);
    /* this method calls other filters in the order they are 
    written in web.xml
    */
  }
}