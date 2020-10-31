package de.ii.xtraplatform.dropwizard.domain;

import javax.servlet.http.HttpServlet;

public interface AdminSubEndpoint {

    String getPath();

    HttpServlet getServlet();
}
