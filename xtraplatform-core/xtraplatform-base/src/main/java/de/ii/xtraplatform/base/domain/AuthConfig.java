/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** @author zahnen */

/** @title Authorization */
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

  /**
   * @en Allow anonymous access to secured resources?
   * @de Anonymen Zugriff auf abgesicherte Ressourcen erlauben?
   * @default `false`
   */
  @Valid @NotNull @JsonProperty public boolean allowAnonymousAccess = false;

  /**
   * @en *HMAC SHA* key for signing the *JSON web tokens*. If not set a new key is generated at
   *     every start and all issued tokens become invalid. The generated key is shown in the log as
   *     a warning and can easily be copied from there into the configuration.
   * @de *HMAC SHA* Schlüssel zu Signierung der *JSON Web Token*. Falls nicht gesetzt wird bei jedem
   *     Start ein neuer Schlüssel generiert und alle ausgegebenen Tokens werden ungültig. Der
   *     generierte Schlüssel wird im Log als Warnung ausgegeben und kann einfach von dort in die
   *     Konfiguration kopiert werden.
   * @default Generated at startup
   */
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
