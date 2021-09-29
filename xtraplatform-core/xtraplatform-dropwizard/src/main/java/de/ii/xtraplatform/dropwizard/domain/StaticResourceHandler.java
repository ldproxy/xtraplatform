package de.ii.xtraplatform.dropwizard.domain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface StaticResourceHandler {

  boolean handle(String path, HttpServletRequest request, HttpServletResponse response);
}
