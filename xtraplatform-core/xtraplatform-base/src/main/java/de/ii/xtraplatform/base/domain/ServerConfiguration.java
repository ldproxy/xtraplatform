/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.server.DefaultServerFactory;

/** @title Webserver */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = ServerConfiguration.class)
public class ServerConfiguration extends DefaultServerFactory {

  private String externalUrl;

  /**
   * @en The [external URL](#external-url) of the web server.
   * @de Die [externe URL](#external-url) des Webservers
   * @default
   */
  @JsonProperty
  public String getExternalUrl() {
    return externalUrl;
  }

  @JsonProperty
  public void setExternalUrl(final String externalUrl) {
    this.externalUrl = externalUrl;
  }
}
