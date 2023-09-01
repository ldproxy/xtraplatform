/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.app.external.XacmlRequest.Version;
import de.ii.xtraplatform.auth.domain.PolicyDecider;
import de.ii.xtraplatform.auth.domain.PolicyDecision;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfiguration.XacmlJson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class PolicyDeciderXacmlJson implements PolicyDecider {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyDeciderXacmlJson.class);

  private static final MediaType XACML = new MediaType("application", "xacml+json");
  private static final MediaType GEOJSON = new MediaType("application", "geo+json");
  private static final ObjectMapper JSON_MAPPER =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final boolean enabled;
  private final String pdpUrl;
  private final XacmlRequest.Version version;
  private final MediaType mediaTypeContent;
  private final MediaType mediaTypeAccept;
  private final boolean geoXacml;

  private final HttpClient httpClient;

  @Inject
  PolicyDeciderXacmlJson(AppContext appContext, Http http) {
    this.httpClient = http.getDefaultClient();

    Optional<XacmlJson> xacmlJson = appContext.getConfiguration().getAuth().getXacmlJson();

    this.enabled = xacmlJson.isPresent();
    this.pdpUrl = xacmlJson.map(XacmlJson::getEndpoint).orElse(null);
    this.version =
        "1.0".equals(xacmlJson.map(XacmlJson::getVersion).orElse(null))
            ? Version._1_0
            : Version._1_1;
    this.mediaTypeContent =
        xacmlJson.map(XacmlJson::getMediaType).map(this::parse).orElse(XACML.withCharset("utf-8"));
    this.mediaTypeAccept = new MediaType(mediaTypeContent.getType(), mediaTypeContent.getSubtype());
    this.geoXacml = xacmlJson.map(XacmlJson::getGeoXacml).orElse(false);
  }

  @Override
  public de.ii.xtraplatform.auth.domain.PolicyDecision request(
      String resourceId,
      Map<String, Object> resourceAttributes,
      String actionId,
      Map<String, Object> actionAttributes,
      Optional<User> user) {
    if (!enabled) {
      throw new IllegalStateException("auth.xacmlJson is disabled");
    }

    try {
      byte[] xacmlRequest =
          getXacmlRequest(resourceId, resourceAttributes, actionId, actionAttributes, user);

      InputStream response =
          httpClient.postAsInputStream(
              pdpUrl, xacmlRequest, mediaTypeContent, Map.of("Accept", mediaTypeAccept.toString()));

      XacmlResponse xacmlResponse = getXacmlResponse(response);

      return evaluate(xacmlResponse);

    } catch (Throwable e) {
      // ignore
      LogContext.error(LOGGER, e, "Error requesting a policy decision");
    }

    return de.ii.xtraplatform.auth.domain.PolicyDecision.deny();
  }

  private byte[] getXacmlRequest(
      String resourceId,
      Map<String, Object> resourceAttributes,
      String actionId,
      Map<String, Object> actionAttributes,
      Optional<User> user)
      throws JsonProcessingException {
    Object xacmlRequest =
        new XacmlRequest(
            version, resourceId, resourceAttributes, actionId, actionAttributes, user, geoXacml);

    LOGGER.debug(
        "XACML {}", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlRequest));

    return JSON_MAPPER.writeValueAsBytes(xacmlRequest);
  }

  private XacmlResponse getXacmlResponse(InputStream response) throws IOException {
    String s = new String(response.readAllBytes(), StandardCharsets.UTF_8);

    XacmlResponse xacmlResponse = JSON_MAPPER.readValue(s, XacmlResponse.class);

    LOGGER.debug("XACML R {}", s);
    LOGGER.debug(
        "XACML R {}",
        JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlResponse));

    return xacmlResponse;
  }

  private MediaType parse(String xacmlJsonMediaType) {
    try {
      return MediaType.valueOf(xacmlJsonMediaType);
    } catch (Throwable e) {
      LOGGER.error("Could not parse xacmlJsonMediaType: {}", xacmlJsonMediaType);
      return XACML.withCharset("utf-8");
    }
  }

  private PolicyDecision evaluate(XacmlResponse xacmlResponse) {

    User.PolicyDecision decision =
        xacmlResponse.isAllowed()
            ? User.PolicyDecision.PERMIT
            : xacmlResponse.isNotApplicable()
                ? User.PolicyDecision.NOT_APPLICABLE
                : xacmlResponse.isIndeterminate()
                    ? User.PolicyDecision.INDETERMINATE
                    : User.PolicyDecision.DENY;
    String status = xacmlResponse.getStatus();

    if (decision == User.PolicyDecision.INDETERMINATE && Objects.nonNull(status)) {
      LOGGER.warn("Indeterminate policy decision ({}).", status);
    }

    return PolicyDecision.of(decision, xacmlResponse.getObligations(), Optional.ofNullable(status));
  }
}
