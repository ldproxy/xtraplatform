/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Authorization
 *     <p>Access to web resources can be restricted using [bearer
 *     tokens](https://tools.ietf.org/html/rfc6750). To validate and evaluate such tokens, an
 *     identity provider (type `OIDC`, `USER_INFO` or `JWT`) has to be defined in the configuration.
 *     Currently only a single identity provider is supported, additional ones are ignored.
 *     <p>An additional authorization layer may be enabled using a [Policy Decision
 *     Point](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html#_Toc525034242).
 *     To do that, a provider with type `XACML_JSON` has to be defined in the configuration.
 *     Currently only a single policy decision provider is supported, additional ones are ignored.
 *     <p>**Provider types**
 *     <p><code>
 * - `OIDC`: identity provider, see [OpenID Connect](#openid-connect)
 * - `USER_INFO`: identity provider, see [User info endpoint](#user-info-endpoint)
 * - `JWT`: identity provider, see [JWT signing key](#jwt-signing-key)
 * - `XACML_JSON`: policy decision provider, see [XACML JSON](#xacml-json)
 *     </code>
 *     <p>**Configuration**
 *     <p>These are the configuration options for key `auth` in `cfg.yml`.
 *     <p>{@docTable:properties}
 *     <p>{@docVar:oidc}
 *     <p>{@docTable:oidc}
 *     <p>{@docVar:userInfo}
 *     <p>{@docTable:userInfo}
 *     <p>{@docVar:jwt}
 *     <p>{@docTable:jwt}
 *     <p>{@docVar:claims}
 *     <p>{@docTable:claims}
 *     <p>{@docVar:login}
 *     <p>{@docTable:login}
 *     <p>{@docVar:xacml}
 *     <p>{@docTable:xacml}
 * @langDe # Autorisierung
 *     <p>Der Zugriff auf Web-Ressourcen kann mithilfe von [Bearer
 *     Token](https://tools.ietf.org/html/rfc6750) beschränkt werden. Um solche Tokens zu validieren
 *     und auszuwerten muss ein Identity-Provider (`type` ist `OIDC`, `USER_INFO` oder `JWT`) in der
 *     Konfiguration definiert werden. Aktuell wird nur ein einziger Identity-Provider unterstützt,
 *     weitere werden ignoriert.
 *     <p>Ein zusätzlicher Autorisierungs-Layer kann mithilfe eines [Policy Decision
 *     Point](https://docs.oasis-open.org/xacml/xacml-rest/v1.1/os/xacml-rest-v1.1-os.html#_Toc525034242)
 *     aktiviert werden. Um das zu tun, muss ein Provider mit `type: XACML_JSON` in der
 *     Konfiguration definiert werden. Aktuell wird nur ein einziger Policy-Decision-Provider
 *     unterstützt, weitere werden ignoriert.
 *     <p>**Provider Typen**
 *     <p><code>
 * - `OIDC`: Identity-Provider, siehe [OpenID Connect](#openid-connect)
 * - `USER_INFO`: Identity-Provider, siehe [User-Info-Endpoint](#user-info-endpoint)
 * - `JWT`: Identity-Provider, siehe [JWT-Signing-Key](#jwt-signing-key)
 * - `XACML_JSON`: Policy-Decision-Provider, siehe [XACML JSON](#xacml-json)
 *     </code>
 *     <p>**Konfiguration**
 *     <p>Dies sind die Konfigurations-Optionen für den Key `auth` in `cfg.yml`.
 *     <p>{@docTable:properties}
 *     <p>{@docVar:oidc}
 *     <p>{@docTable:oidc}
 *     <p>{@docVar:userInfo}
 *     <p>{@docTable:userInfo}
 *     <p>{@docVar:jwt}
 *     <p>{@docTable:jwt}
 *     <p>{@docVar:claims}
 *     <p>{@docTable:claims}
 *     <p>{@docVar:login}
 *     <p>{@docTable:login}
 *     <p>{@docVar:xacml}
 *     <p>{@docTable:xacml}
 * @ref:cfgProperties {@link de.ii.xtraplatform.base.domain.ImmutableAuthConfiguration}
 * @ref:oidc {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Oidc}
 * @ref:oidcTable {@link de.ii.xtraplatform.base.domain.ImmutableOidc}
 * @ref:userInfo {@link de.ii.xtraplatform.base.domain.AuthConfiguration.UserInfo}
 * @ref:userInfoTable {@link de.ii.xtraplatform.base.domain.ImmutableUserInfo}
 * @ref:jwt {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Jwt}
 * @ref:jwtTable {@link de.ii.xtraplatform.base.domain.ImmutableJwt}
 * @ref:claims {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Claims}
 * @ref:claimsTable {@link de.ii.xtraplatform.base.domain.ImmutableClaims}
 * @ref:login {@link de.ii.xtraplatform.base.domain.AuthConfiguration.Login}
 * @ref:loginTable {@link de.ii.xtraplatform.base.domain.ImmutableLogin}
 * @ref:xacml {@link de.ii.xtraplatform.base.domain.AuthConfiguration.XacmlJson}
 * @ref:xacmlTable {@link de.ii.xtraplatform.base.domain.ImmutableXacmlJson}
 */
@DocFile(
    path = "application/20-configuration",
    name = "40-auth.md",
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
          name = "userInfo",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:userInfoTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "jwt",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:jwtTable}"),
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
          name = "login",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:loginTable}"),
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
          name = "userInfo",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:userInfo}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "jwt",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:jwt}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "claims",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:claims}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "login",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:login}"),
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
  // TODO: could be replaced with property obligations
  @DocIgnore
  Optional<String> getPostProcessingEndpoint();

  default Optional<Oidc> getOidc() {
    return getProviders().values().stream()
        .filter(authProvider -> authProvider instanceof IdentityProvider)
        .findFirst()
        .filter(authProvider -> authProvider.getType() == AuthProviderType.OIDC)
        .map(Oidc.class::cast);
  }

  default Optional<UserInfo> getUserInfo() {
    return getProviders().values().stream()
        .filter(authProvider -> authProvider instanceof IdentityProvider)
        .findFirst()
        .filter(authProvider -> authProvider.getType() == AuthProviderType.USER_INFO)
        .map(UserInfo.class::cast);
  }

  default Optional<Jwt> getJwt() {
    return getProviders().values().stream()
        .filter(authProvider -> authProvider instanceof IdentityProvider)
        .findFirst()
        .filter(authProvider -> authProvider.getType() == AuthProviderType.JWT)
        .map(Jwt.class::cast);
  }

  default Optional<XacmlJson> getXacmlJson() {
    return getProviders().values().stream()
        .filter(authProvider -> authProvider.getType() == AuthProviderType.XACML_JSON)
        .map(XacmlJson.class::cast)
        .findFirst();
  }

  /**
   * @langEn A map with provider definitions. Keys are user-defined ids used for referencing, values
   *     are provider definitions with a `type`. See above for supported types.
   * @langDe Eine Map mit Provider-Definitionen. Keys sind Nutzer-definierte Ids, Werte sind
   *     Provider-Definitionen mit einem `type`. Siehe oben für unterstützte Typen.
   * @since v3.5
   * @default {}
   */
  Map<String, AuthProvider> getProviders();

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
   * @langAll
   *     <p><code>
   * ```yaml
   * auth:
   *   providers:
   *     oidc-ldproxy-integrated:
   *       type: OIDC
   *       endpoint: https://my-keycloak/realms/ldproxy/.well-known/openid-configuration
   *       login:
   *         clientId: ldproxy-integrated
   *       claims:
   *         userName: preferred_username
   * ```
   * </code>
   */
  @Value.Immutable
  @JsonDeserialize(builder = ImmutableOidc.Builder.class)
  interface Oidc extends AuthProvider, IdentityProvider, LoginProvider {

    /**
     * @langEn Always `OIDC`.
     * @langDe Immer `OIDC`.
     * @since v3.5
     */
    @Value.Default
    @Override
    default AuthProviderType getType() {
      return AuthProviderType.OIDC;
    }

    @Value.Default
    @Override
    default ImmutableClaims getClaims() {
      return IdentityProvider.super.getClaims();
    }

    @Override
    Optional<Login> getLogin();

    /**
     * @langEn The OpenID Connect configuration endpoint, a URL ending with
     *     `.well-known/openid-configuration`.
     * @langDe Der OpenID Connect Konfigurationsendpunkt, eine URL endend mit
     *     `.well-known/openid-configuration`.
     * @since v3.5
     */
    String getEndpoint();
  }

  /**
   * @langEn ## User info endpoint
   *     <p>An endpoint that is responsible for validating a token and returning the required
   *     claims.
   * @langDe ## User-Info-Endpoint
   *     <p>Ein Endpoint der dafür verantwortlich ist, das Token zu validieren und die benötigten
   *     Claims zurückzuliefern.
   * @langAll
   *     <p><code>
   * ```yaml
   * auth:
   *   providers:
   *     userinfo-custom:
   *       type: USER_INFO
   *       endpoint: https://my-userinfo-endpoint?token={token}
   *       claims:
   *         userName: name
   * ```
   * </code>
   */
  @Value.Immutable
  @JsonDeserialize(builder = ImmutableUserInfo.Builder.class)
  interface UserInfo extends AuthProvider, IdentityProvider {

    /**
     * @langEn Always `USER_INFO`.
     * @langDe Immer `USER_INFO`.
     * @since v3.5
     */
    @Override
    AuthProviderType getType();

    @Value.Default
    @Override
    default ImmutableClaims getClaims() {
      return IdentityProvider.super.getClaims();
    }

    /**
     * @langEn A URL template, `{token}` is replaced with the token.
     * @langDe Ein URL-Template, `{token}` wird durch das Token ersetzt.
     * @since v3.5
     */
    String getEndpoint();
  }

  /**
   * @langEn ## JWT signing key
   *     <p>A signing key is used to validate JSON Web Tokens and the claims are extracted directly
   *     from the token.
   * @langDe ## JWT-Signing-Key
   *     <p>Ein Signing-Key wird verwendet um JSON Web Tokens zu validieren und die Claims werden
   *     direkt aus dem Token extrahiert.
   * @langAll
   *     <p><code>
   * ```yaml
   * auth:
   *   providers:
   *     jwt-custom:
   *       type: JWT
   *       signingKey: 'nurrK3JeUC3ccqs5CESFzgjCsCj3omS+PxDvMeSngqM='
   *       claims:
   *         userName: user
   * ```
   * </code>
   */
  @Value.Immutable
  @JsonDeserialize(builder = ImmutableJwt.Builder.class)
  interface Jwt extends AuthProvider, IdentityProvider {

    /**
     * @langEn Always `JWT`.
     * @langDe Immer `JWT`.
     * @since v3.5
     */
    @Override
    AuthProviderType getType();

    @Value.Default
    @Override
    default ImmutableClaims getClaims() {
      return IdentityProvider.super.getClaims();
    }

    /**
     * @langEn Signing key for JSON Web Tokens.
     * @langDe Signing-Key für JSON Web Tokens.
     * @since v3.5
     */
    String getSigningKey();
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
  @JsonDeserialize(builder = ImmutableClaims.Builder.class)
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
   * @langEn ## Login
   *     <p>This allows API clients that are integrated in ldproxy to automatically redirect to the
   *     login form of the identity provider.
   * @langDe ## Login
   *     <p>Dies erlaubt in ldproxy integrierten API-Clients die automatische Weiterleitung zum
   *     Login-Formular des Identity-Providers.
   */
  @Value.Immutable
  @JsonDeserialize(builder = ImmutableLogin.Builder.class)
  interface Login {
    /**
     * @langEn A client id that is registered with the identity provider. The corresponding client
     *     has to support [Authorization Code
     *     Flow](https://openid.net/specs/openid-connect-core-1_0.html#CodeFlowAuth).
     * @langDe Eine Client-Id die im Identity-Provider registriert ist. Der zugehörige Client muss
     *     [Authorization Code
     *     Flow](https://openid.net/specs/openid-connect-core-1_0.html#CodeFlowAuth) unterstützen.
     * @since v3.5
     */
    String getClientId();

    /**
     * @langEn Optional client secret for the given client id.
     * @langDe Optionales Client-Secret für die angegebene Client-Id.
     * @since v3.5
     * @default null
     */
    Optional<String> getClientSecret();
  }

  enum AuthProviderType {
    OIDC,
    USER_INFO,
    JWT,
    XACML_JSON
  }

  @SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
  String OIDC = "OIDC";

  @SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
  String USER_INFO = "USER_INFO";

  @SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
  String JWT = "JWT";

  @SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
  String XACML_JSON = "XACML_JSON";

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Oidc.class, name = OIDC),
    @JsonSubTypes.Type(value = UserInfo.class, name = USER_INFO),
    @JsonSubTypes.Type(value = Jwt.class, name = JWT),
    @JsonSubTypes.Type(value = XacmlJson.class, name = XACML_JSON),
  })
  interface AuthProvider {
    AuthProviderType getType();
  }

  interface IdentityProvider {
    /**
     * @langEn Mapping of token claims to ldproxy claims, see [Claims Mapping](#claims-mapping).
     * @langDe Mapping von Token-Claims zu ldproxy-Claims, siehe [Claims Mapping](#claims-mapping).
     * @since v3.5
     * @default see below
     */
    @Value.Default
    default ImmutableClaims getClaims() {
      return new ImmutableClaims.Builder().build();
    }
  }

  interface LoginProvider {
    /**
     * @langEn Login settings, see [Login](#login).
     * @langDe Login Einstellungen, siehe [Login](#login).
     * @since v3.5
     * @default null
     */
    Optional<Login> getLogin();
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
   * @langAll
   *     <p><code>
   * ```yaml
   * auth:
   *   providers:
   *     policies:
   *       type: XACML_JSON
   *       endpoint: https://my-authzforce/policies/domains/ldproxy/pdp
   *       version: 1.0
   * ```
   * </code>
   */
  @Value.Immutable
  @JsonDeserialize(builder = ImmutableXacmlJson.Builder.class)
  interface XacmlJson extends AuthProvider {

    /**
     * @langEn Always `XACML_JSON`.
     * @langDe Immer `XACML_JSON`.
     * @since v3.5
     */
    @Override
    AuthProviderType getType();

    /**
     * @langEn The Policy Decision Point.
     * @langDe Der Policy Decision Point.
     * @since v3.5
     */
    String getEndpoint();

    /**
     * @langEn XACML JSON version, either `1.1` or `1.0`.
     * @langDe XACML JSON Version, entweder `1.1` oder `1.0`.
     * @since v3.5
     * @default 1.1
     */
    @Value.Default
    default XacmlJsonVersion getVersion() {
      return XacmlJsonVersion.V1_1;
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
    V1_0("1.0"),
    V1_1("1.1");
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
      return V1_1;
    }
  }

  enum GeoXacmlVersion {
    NONE("NONE"),
    V1_0("1.0"),
    V3_0("3.0");
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
