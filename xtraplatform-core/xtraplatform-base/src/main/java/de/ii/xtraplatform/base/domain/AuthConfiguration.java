/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
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

  // TODO
  @JsonIgnore
  @Value.Derived
  default boolean isUserInfo() {
    Optional<String> userInfoEndpoint = getSimple().flatMap(Simple::getUserInfoEndpoint);
    try {
      return (userInfoEndpoint.isPresent()
          && new URI(userInfoEndpoint.get().replace("{{token}}", "token")).isAbsolute());
    } catch (URISyntaxException e) {
      return false;
    }
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
  @Deprecated(since = "3.5")
  @Nullable
  String getJwtSigningKey();

  @Deprecated(since = "3.5")
  @Nullable
  String getUserNameKey();

  @Deprecated(since = "3.5")
  @JsonIgnore
  @Value.Derived
  default String getUserRoleKey() {
    return "role";
  }

  @Deprecated(since = "3.5")
  @JsonAlias("userScopesKey")
  @Nullable
  String getUserPermissionsKey();

  @Deprecated(since = "3.5")
  Optional<String> getUserInfoEndpoint();

  @Deprecated(since = "3.5")
  @JsonAlias("externalDynamicAuthorizationEndpoint")
  Optional<String> getXacmlJsonEndpoint();

  // TODO
  Optional<String> getPostProcessingEndpoint();

  @Deprecated(since = "3.5")
  @Nullable
  String getXacmlJsonVersion();

  @Deprecated(since = "3.5")
  @Nullable
  String getXacmlJsonMediaType();

  @Deprecated(since = "3.5")
  @Value.Check
  default AuthConfiguration backwardsCompatibility() {

    if (Objects.nonNull(getUserNameKey()) || Objects.nonNull(getUserPermissionsKey())) {
      ImmutableClaims.Builder builder = new ImmutableClaims.Builder();
      if (Objects.nonNull(getUserNameKey())) {
        builder.userName(getUserNameKey());
      }
      if (Objects.nonNull(getUserPermissionsKey())) {
        builder.permissions(getUserPermissionsKey());
      }

      return new ImmutableAuthConfiguration.Builder()
          .from(this)
          .claims(builder.build())
          .userNameKey(null)
          .userPermissionsKey(null)
          .build();
    }

    if (Objects.nonNull(getJwtSigningKey()) || getUserInfoEndpoint().isPresent()) {
      ImmutableSimple.Builder builder = new ImmutableSimple.Builder();
      if (Objects.nonNull(getJwtSigningKey())) {
        builder.jwtSigningKey(getJwtSigningKey());
      }
      if (getUserInfoEndpoint().isPresent()) {
        builder.userInfoEndpoint(getUserInfoEndpoint().get());
      }

      return new ImmutableAuthConfiguration.Builder()
          .from(this)
          .simple(builder.build())
          .jwtSigningKey(null)
          .userInfoEndpoint(Optional.empty())
          .build();
    }

    if (getXacmlJsonEndpoint().isPresent()
        || Objects.nonNull(getXacmlJsonVersion())
        || Objects.nonNull(getXacmlJsonMediaType())) {
      ImmutableXacmlJson.Builder builder = new ImmutableXacmlJson.Builder();
      if (getXacmlJsonEndpoint().isPresent()) {
        builder.endpoint(getXacmlJsonEndpoint().get());
      }
      if (Objects.nonNull(getXacmlJsonVersion())) {
        builder.version(XacmlJsonVersion.fromString(getXacmlJsonVersion()));
      }
      if (Objects.nonNull(getUserPermissionsKey())) {
        builder.mediaType(getXacmlJsonMediaType());
      }

      return new ImmutableAuthConfiguration.Builder()
          .from(this)
          .xacmlJson(builder.build())
          .xacmlJsonEndpoint(Optional.empty())
          .xacmlJsonVersion(null)
          .xacmlJsonMediaType(null)
          .build();
    }

    return this;
  }

  @Value.Check
  default AuthConfiguration defaults() {
    if (Objects.isNull(getClaims())) {
      return new ImmutableAuthConfiguration.Builder()
          .from(this)
          .claims(ModifiableClaims.create())
          .build();
    }

    return this;
  }

  Optional<Oidc> getOidc();

  Optional<Simple> getSimple();

  Optional<XacmlJson> getXacmlJson();

  @Nullable
  Claims getClaims();

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableOidc.class)
  interface Oidc {
    String getEndpoint();

    String getClientId();

    Optional<String> getClientSecret();
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableSimple.class)
  interface Simple {
    Optional<String> getJwtSigningKey();

    Optional<String> getUserInfoEndpoint();
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableClaims.class)
  interface Claims {
    @Value.Default
    default String getUserName() {
      return "sub";
    }

    @Value.Default
    default String getAudience() {
      return "aud";
    }

    @Value.Default
    default String getScopes() {
      return "scope";
    }

    @Value.Default
    default String getPermissions() {
      return "roles";
    }
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableXacmlJson.class)
  interface XacmlJson {
    String getEndpoint();

    @Value.Default
    default XacmlJsonVersion getVersion() {
      return XacmlJsonVersion._1_1;
    }

    @Value.Default
    default String getMediaType() {
      return "application/xacml+json;charset=UTF-8";
    }

    @Value.Default
    default GeoXacmlVersion getGeoXacmlVersion() {
      return GeoXacmlVersion.NONE;
    }
  }

  enum XacmlJsonVersion {
    _1_0("1.0"),
    _1_1("1.1");
    private final String stringRepresentation;

    XacmlJsonVersion(String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString() {
      return stringRepresentation;
    }

    @JsonCreator
    public static XacmlJsonVersion fromString(String type) {
      for (XacmlJsonVersion v : XacmlJsonVersion.values()) {
        if (v.toString().equals(type)) {
          return v;
        }
      }
      return _1_1;
    }
  }

  enum GeoXacmlVersion {
    NONE("NONE"),
    _1_0("1.0"),
    _3_0("3.0");
    private final String stringRepresentation;

    GeoXacmlVersion(String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString() {
      return stringRepresentation;
    }

    @JsonCreator
    public static GeoXacmlVersion fromString(String type) {
      for (GeoXacmlVersion v : GeoXacmlVersion.values()) {
        if (v.toString().equals(type)) {
          return v;
        }
      }
      return NONE;
    }
  }
}
