/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.server.DefaultServerFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = XtraPlatformServerFactory.class)
public class XtraPlatformServerFactory extends DefaultServerFactory {
  private String externalUrl;

  @JsonProperty
  public String getExternalUrl() {
    return externalUrl;
  }

  @JsonProperty
  public void setExternalUrl(final String externalUrl) {
    this.externalUrl = externalUrl;
  }
}
