/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import io.dropwizard.views.common.View;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fischer
 */
public class GenericView extends View {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericView.class);

  private final URI uri;
  private Object data;

  public GenericView(String template, URI uri) {
    super(template + ".mustache");
    this.uri = uri;
    this.data = null;
  }

  public GenericView(String template, URI uri, Object data) {
    this(template, uri);
    this.data = data;
  }

  public String getPath() {
    if (uri.getPath().endsWith("/")) {
      return "";
    } else {
      return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1) + "/";
    }
  }

  public String getQuery() {

    return "?" + (uri.getQuery() != null ? uri.getQuery() + "&" : "");
  }

  public String getRawQuery() {

    return "?" + (uri.getRawQuery() != null ? uri.getRawQuery() + "&" : "");
  }

  public Object getData() {
    return data;
  }
}
