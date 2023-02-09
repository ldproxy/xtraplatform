/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @title Authorization
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAuthConfiguration.class)
public interface AuthConfiguration {

  @JsonIgnore
  @Value.Derived
  default boolean isActive() {
    try {
      return (getUserInfoEndpoint().isPresent()
          && new URI(getUserInfoEndpoint().get().replace("{{token}}", "token")).isAbsolute());
    } catch (URISyntaxException e) {
      return false;
    }
  }

  // TODO
  @JsonIgnore
  @Value.Derived
  default boolean getAllowAnonymousAccess() {
    return false;
  }

  /**
   * @langEn *HMAC SHA* key for signing the *JSON web tokens*. If not set a new key is generated at
   *     every start and all issued tokens become invalid. The generated key is shown in the log as
   *     a warning and can easily be copied from there into the configuration.
   * @langDe *HMAC SHA* Schlüssel zu Signierung der *JSON Web Token*. Falls nicht gesetzt wird bei
   *     jedem Start ein neuer Schlüssel generiert und alle ausgegebenen Tokens werden ungültig. Der
   *     generierte Schlüssel wird im Log als Warnung ausgegeben und kann einfach von dort in die
   *     Konfiguration kopiert werden.
   * @default Generated at startup
   */
  @Nullable
  String getJwtSigningKey();

  @Value.Default
  default String getUserNameKey() {
    return "name";
  }

  @JsonIgnore
  @Value.Derived
  default String getUserRoleKey() {
    return "role";
  }

  @Value.Default
  default String getUserScopesKey() {
    return "role";
  }

  Optional<String> getUserInfoEndpoint();

  Optional<String> getConnectionInfoEndpoint();

  Optional<String> getExternalDynamicAuthorizationEndpoint();

  Optional<String> getPostProcessingEndpoint();

  @Value.Default
  default String getXacmlJsonVersion() {
    return "1.1";
  }

  @Value.Default
  default String getXacmlJsonMediaType() {
    return "application/xacml+json;charset=UTF-8";
  }
}
