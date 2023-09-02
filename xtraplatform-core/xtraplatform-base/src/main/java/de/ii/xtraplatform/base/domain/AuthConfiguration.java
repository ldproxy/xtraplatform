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
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn # Authorization
 *     <p>Access to web resources can be restricted using [bearer
 *     tokens](https://tools.ietf.org/html/rfc6750). The configuration options determine how such
 *     tokens are validated and evaluated.
 *     <p>An additional authorization layer may be enabled using a [Policy Decision
 *     Point](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html#_Toc525034242).
 *     <p>{@docTable:properties}
 *     <p>{@docVar:oidc}
 *     <p>{@docTable:oidc}
 *     <p>{@docVar:simple}
 *     <p>{@docTable:simple}
 *     <p>{@docVar:claims}
 *     <p>{@docTable:claims}
 *     <p>{@docVar:xacml}
 *     <p>{@docTable:xacml}
 * @langDe # Autorisierung
 *     <p>Der Zugriff auf Web-Ressourcen kann mithilfe von [Bearer
 *     Token](https://tools.ietf.org/html/rfc6750) beschränkt werden. Die Konfigurations-Optionen
 *     bestimmen, wie solche Tokens validiert und ausgewertet werden.
 *     <p>Ein zusätzlicher Autorisierungs-Layer kann mithilfe eines [Policy Decision
 *     Point](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html#_Toc525034242)
 *     aktiviert werden.
 *     <p>{@docTable:properties}
 *     <p>{@docVar:oidc}
 *     <p>{@docTable:oidc}
 *     <p>{@docVar:simple}
 *     <p>{@docTable:simple}
 *     <p>{@docVar:claims}
 *     <p>{@docTable:claims}
 *     <p>{@docVar:xacml}
 *     <p>{@docTable:xacml}
 * @ref:cfgProperties {@link de.ii.xtraplatform.base.domain.ImmutableAuthConfiguration}
 * @ref:oidc {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Oidc}
 * @ref:oidcTable {@link de.ii.xtraplatform.base.domain.ImmutableOidc}
 * @ref:simple {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Simple}
 * @ref:simpleTable {@link de.ii.xtraplatform.base.domain.ImmutableSimple}
 * @ref:claims {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Claims}
 * @ref:claimsTable {@link de.ii.xtraplatform.base.domain.ImmutableClaims}
 * @ref:xacml {@link de.ii.xtraplatform.base.domain.AuthConfiguration.XacmlJson}
 * @ref:xacmlTable {@link de.ii.xtraplatform.base.domain.ImmutableXacmlJson}
 */
@DocFile(
    path = "application",
    name = "65-auth.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "oidc",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:oidcTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "simple",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:simpleTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "claims",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:claimsTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "xacml",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:xacmlTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    },
    vars = {
      @DocVar(
          name = "oidc",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:oidc}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "simple",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:simple}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "claims",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:claims}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "xacml",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:xacml}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
    })
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

  @DocIgnore
  @Deprecated(since = "3.5")
  @Nullable
  String getJwtSigningKey();

  @DocIgnore
  @Deprecated(since = "3.5")
  @Nullable
  String getUserNameKey();

  @DocIgnore
  @Deprecated(since = "3.5")
  @JsonIgnore
  @Value.Derived
  default String getUserRoleKey() {
    return "role";
  }

  @DocIgnore
  @Deprecated(since = "3.5")
  @JsonAlias("userScopesKey")
  @Nullable
  String getUserPermissionsKey();

  @DocIgnore
  @Deprecated(since = "3.5")
  Optional<String> getUserInfoEndpoint();

  @DocIgnore
  @Deprecated(since = "3.5")
  @JsonAlias("externalDynamicAuthorizationEndpoint")
  Optional<String> getXacmlJsonEndpoint();

  // TODO: could be replaced with property obligations
  @DocIgnore
  Optional<String> getPostProcessingEndpoint();

  @DocIgnore
  @Deprecated(since = "3.5")
  @Nullable
  String getXacmlJsonVersion();

  @DocIgnore
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

  /**
   * @langEn OpenID Connect settings, see [OpenId Connect](#openid-connect).
   * @langDe OpenID Connect Einstellungen, siehe [OpenId Connect](#openid-connect).
   * @since v3.5
   * @default {}
   */
  Optional<Oidc> getOidc();

  /**
   * @langEn Settings for other protocols, see [Simple](#simple).
   * @langDe Einstellungen für andere Protokolle, siehe [Simple](#simple).
   * @since v3.5
   * @default {}
   */
  Optional<Simple> getSimple();

  /**
   * @langEn Mapping of token claims to ldproxy claims, see [Claims Mapping](#claims-mapping).
   * @langDe Mapping von Token-Claims zu ldproxy-Claims, siehe [Claims Mapping](#claims-mapping).
   * @since v3.5
   * @default see below
   */
  @Nullable
  Claims getClaims();

  /**
   * @langEn XACML JSON PDP settings, see [XACML JSON](#xacml-json).
   * @langDe XACML JSON PDP Einstellungen, siehe [XACML JSON](#xacml-json).
   * @since v3.5
   * @default {}
   */
  Optional<XacmlJson> getXacmlJson();

  /**
   * @langEn ## OpenID Connect
   *     <p>With [OpenID Connect](https://openid.net/developers/how-connect-works/), the signed JSON
   *     Web Token is validated using the certificates provided by the configuration endpoint and
   *     the claims are extracted directly from the token.
   *     <p>A common open source implementation is [Keycloak](https://www.keycloak.org).
   * @langDe ## OpenID Connect
   *     <p>Bei [OpenID Connect](https://openid.net/developers/how-connect-works/) werden die
   *     signierten JSON Web Token mithilfe der Zertifikate validiert, die der
   *     Konfigurations-Endpunkt bereitstellt, und die Claims werden direkt aus dem Token
   *     extrahiert.
   *     <p>Eine verbreitete Open-Source Implementierung ist [Keycloak](https://www.keycloak.org).
   */
  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableOidc.class)
  interface Oidc {
    /**
     * @langEn The OpenID Connect configuration endpoint, a URL ending with
     *     `.well-known/openid-configuration`.
     * @langDe Der OpenID Connect Konfigurationsendpunkt, eine URL endend mit
     *     `.well-known/openid-configuration`.
     * @since v3.5
     */
    String getEndpoint();

    /**
     * @langEn The client id used in requests to the OpenID Connect provider.
     * @langDe Die Client-Id die in Requests an den OpenID Connect Provider verwendet wird.
     * @since v3.5
     */
    String getClientId();

    /**
     * @langEn Optional client secret.
     * @langDe Optionales Client-Secret.
     * @since v3.5
     * @default null
     */
    Optional<String> getClientSecret();
  }

  /**
   * @langEn ## Simple
   *     <p>Without OpenID Connect, either a signing key for JSON Web Tokens may be provided or an
   *     endpoint can be defined that is responsible for validating a token and returning the
   *     required claims.
   * @langDe ## Simple
   *     <p>Ohne OpenID Connect kann entweder ein Signing-Key für JSON Web Tokens angegeben werden
   *     oder ein Endpunkt der dafür verantwortlich ist, das Token zu validieren und die benötigten
   *     Claims zurückzuliefern.
   */
  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableSimple.class)
  interface Simple {
    /**
     * @langEn Signing key for JSON Web Tokens.
     * @langDe Signing-Key für JSON Web Tokens.
     * @since v3.5
     * @default null
     */
    Optional<String> getJwtSigningKey();

    /**
     * @langEn User info endpoint.
     * @langDe User-Info-Endpunkt.
     * @since v3.5
     * @default null
     */
    Optional<String> getUserInfoEndpoint();
  }

  /**
   * @langEn ## Claims Mapping
   *     <p>This defines how ldproxy can extract required information from a token. The values need
   *     to match the claims in the token. Nested JSON objects are supported, the values can be a
   *     path like `foo.bar`.
   * @langDe ## Claims Mapping
   *     <p>Hier wird definiert, wie ldproxy benötigte Informationen aus dem Token extrahieren kann.
   *     Die Werte müssen Claims im Token entsprechen. Verschachtelte JSON Objekte werden
   *     unterstützt, die Werte können einen Pfad wie `foo.bar` sein.
   */
  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableClaims.class)
  interface Claims {
    /**
     * @langEn The claim containing the user name.
     * @langDe Der Claim, der den Benutzernamen enthält.
     * @since v3.5
     * @default sub
     */
    @Value.Default
    default String getUserName() {
      return "sub";
    }

    /**
     * @langEn The claim containing the user permissions.
     * @langDe Der Claim, der die Berechtigungen des Benutzers enthält.
     * @since v3.5
     * @default roles
     */
    @Value.Default
    default String getPermissions() {
      return "roles";
    }

    /**
     * @langEn The claim containing the audience.
     * @langDe Der Claim, der die Zielgruppe (Audience) enthält.
     * @since v3.5
     * @default aud
     */
    @Value.Default
    default String getAudience() {
      return "aud";
    }

    /**
     * @langEn The claim containing the scopes.
     * @langDe Der Claim, der die Gültigkeitsbereiche (Scopes) enthält.
     * @since v3.5
     * @default scope
     */
    @Value.Default
    default String getScopes() {
      return "scope";
    }
  }

  /**
   * @langEn ## XACML JSON
   *     <p>Policy Decision Points implementing [XACML
   *     3.0](https://docs.oasis-open.org/xacml/3.0/xacml-3.0-core-spec-os-en.html), [XACML REST
   *     1.1](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html) and
   *     [XACML JSON
   *     1.1](https://docs.oasis-open.org/xacml/xacml-json-http/v1.1/os/xacml-json-http-v1.1-os.html)
   *     or [XACML JSON
   *     1.0](http://docs.oasis-open.org/xacml/xacml-json-http/v1.0/xacml-json-http-v1.0.html) are
   *     supported.
   *     <p>A common open source implementation is [AuthzForce Server (Community
   *     Edition)](https://github.com/authzforce/server).
   * @langDe ## XACML JSON
   *     <p>Policy Decision Points die [XACML
   *     3.0](https://docs.oasis-open.org/xacml/3.0/xacml-3.0-core-spec-os-en.html), [XACML REST
   *     1.1](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html) und
   *     [XACML JSON
   *     1.1](https://docs.oasis-open.org/xacml/xacml-json-http/v1.1/os/xacml-json-http-v1.1-os.html)
   *     oder [XACML JSON
   *     1.0](http://docs.oasis-open.org/xacml/xacml-json-http/v1.0/xacml-json-http-v1.0.html)
   *     implementieren werden unterstützt.
   *     <p>Eine verbreitete Open-Source Implementierung ist [AuthzForce Server (Community
   *     Edition)](https://github.com/authzforce/server).
   */
  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableXacmlJson.class)
  interface XacmlJson {
    String getEndpoint();

    /**
     * @langEn XACML JSON version, either `1.1` or `1.0`.
     * @langDe XACML JSON Version, entweder `1.1` oder `1.0`.
     * @since v3.5
     * @default 1.1
     */
    @Value.Default
    default XacmlJsonVersion getVersion() {
      return XacmlJsonVersion._1_1;
    }

    /**
     * @langEn Media type for XACML JSON used by the PDP.
     * @langDe Media type für XACML JSON, den der PDP verwendet.
     * @since v3.5
     * @default application/xacml+json;charset=UTF-8
     */
    @Value.Default
    default String getMediaType() {
      return "application/xacml+json;charset=UTF-8";
    }

    /**
     * @langEn Optional support for [GeoXACML 3.0](https://docs.ogc.org/DRAFTS/22-049.html) or
     *     [GeoXACML 1.0](https://www.ogc.org/standard/geoxacml/). If unset or `NONE`, geometries
     *     will be sent with type `string` in XACML requests, if `3.0` or `1.0` the corresponding
     *     GeoXACML type will be used.
     * @langDe Optionale Unterstützung für [GeoXACML 3.0](https://docs.ogc.org/DRAFTS/22-049.html)
     *     oder [GeoXACML 1.0](https://www.ogc.org/standard/geoxacml/). Wenn nicht gesetzt oder
     *     `NONE`, werden Geometrien mit Typ `string` in XACML Requests gesendet, wenn `3.0` oder
     *     `1.0` wird der entsprechende GeoXACML Typ verwendet.
     * @since v3.5
     * @default NONE
     */
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
