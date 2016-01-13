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