/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** @author zahnen */
public class AuthConfig {

  @JsonIgnore
  public boolean isJwt() {
    return Objects.nonNull(Strings.emptyToNull(jwtSigningKey));
  }

  @JsonIgnore
  public boolean isActive() {
    try {
      return (isJwt() || new URI(getUserInfoEndpoint.replace("{{token}}", "token")).isAbsolute());
    } catch (URISyntaxException e) {
      return false;
    }
  }

  @Valid @NotNull @JsonProperty public boolean isAnonymousAccessAllowed = false;

  @Valid
  // @NotNull
  @JsonProperty
  public String jwtSigningKey;

  @Valid @NotNull @JsonProperty public String getUserNameKey = "name";

  @Valid @NotNull @JsonProperty public String getUserRoleKey = "role";

  @Valid @NotNull @JsonProperty public String getUserInfoEndpoint = "";

  @Valid @NotNull @JsonProperty public String getConnectionInfoEndpoint = "";

  @Valid @NotNull @JsonProperty public String getExternalDynamicAuthorizationEndpoint = "";

  @Valid @NotNull @JsonProperty public String getPostProcessingEndpoint = "";
}
