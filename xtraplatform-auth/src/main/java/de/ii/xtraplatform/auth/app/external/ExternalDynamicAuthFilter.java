/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.User.PolicyDecision;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.HttpClient;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@PreMatching
public class ExternalDynamicAuthFilter<P extends Principal> extends AuthFilter<String, P> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDynamicAuthFilter.class);

  private static final MediaType XACML = new MediaType("application", "xacml+json");
  private static final MediaType GEOJSON = new MediaType("application", "geo+json");
  private static final ObjectMapper JSON =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final String edaUrl;
  private final boolean xacmlJson10;
  private final MediaType mediaType;
  private final MediaType mediaTypeAccept;
  private final String ppUrl;
  private final HttpClient httpClient;
  private final OAuthCredentialAuthFilter<P> delegate;

  ExternalDynamicAuthFilter(
      String edaUrl,
      String xacmlJsonVersion,
      String xacmlJsonMediaType,
      String ppUrl,
      HttpClient httpClient,
      OAuthCredentialAuthFilter<P> delegate) {
    super();
    this.realm = "ldproxy";
    this.prefix = "Bearer";
    this.unauthorizedHandler = new DefaultUnauthorizedHandler();

    this.edaUrl = edaUrl;
    this.xacmlJson10 = "1.0".equals(xacmlJsonVersion);
    this.mediaType = parse(xacmlJsonMediaType);
    this.mediaTypeAccept = new MediaType(mediaType.getType(), mediaType.getSubtype());
    this.ppUrl = ppUrl;
    this.httpClient = httpClient;
    this.delegate = delegate;
  }

  private MediaType parse(String xacmlJsonMediaType) {
    try {
      return MediaType.valueOf(xacmlJsonMediaType);
    } catch (Throwable e) {
      LOGGER.error("Could not parse xacmlJsonMediaType: {}", xacmlJsonMediaType);
      return XACML.withCharset("utf-8");
    }
  }

  // TODO: cfg.yml
  static List<String> METHODS = ImmutableList.of("GET", "POST", "PUT", "DELETE");

  // TODO: either interface that is implemented in ogcapi to which we pass service id
  // or add xacml request as callback to user
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    SecurityContext oldSecurityContext = requestContext.getSecurityContext();

    delegate.filter(requestContext);

    if (METHODS.contains(requestContext.getMethod())) {

      List<String> pathSegments =
          Splitter.on('/').omitEmptyStrings().splitToList(requestContext.getUriInfo().getPath());
      int serviceIndex = pathSegments.indexOf("services");

      if (serviceIndex >= 0 && pathSegments.size() > serviceIndex) {
        PolicyDecision policyDecision =
            askPDP(
                requestContext.getSecurityContext().getUserPrincipal().getName(),
                requestContext.getMethod(),
                "/"
                    + Joiner.on('/')
                        .join(pathSegments.subList(serviceIndex + 1, pathSegments.size())),
                getEntityBody(requestContext));

        User user =
            ImmutableUser.builder()
                .from(requestContext.getSecurityContext().getUserPrincipal())
                .policyDecision(policyDecision)
                .build();

        requestContext.setSecurityContext(
            new SecurityContext() {
              public Principal getUserPrincipal() {
                return user;
              }

              public boolean isUserInRole(String role) {
                return requestContext.getSecurityContext().isUserInRole(role);
              }

              public boolean isSecure() {
                return requestContext.getSecurityContext().isSecure();
              }

              public String getAuthenticationScheme() {
                return requestContext.getSecurityContext().getAuthenticationScheme();
              }
            });

        if (policyDecision == PolicyDecision.DENY) {
          // reset security context, because we use @PermitAll and then decide based on Principal
          // existence
          // requestContext.setSecurityContext(oldSecurityContext);
          // is ignored for @PermitAll
          throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
        }

        if (!ppUrl.isEmpty()) {
          postProcess(requestContext, getEntityBody(requestContext));
        }
      }
    }
  }

  private void postProcess(ContainerRequestContext requestContext, byte[] body) {
    if (requestContext.getMethod().equals("POST") || requestContext.getMethod().equals("PUT")) {
      try {

        InputStream processedBody =
            httpClient.postAsInputStream(
                ppUrl, body, GEOJSON.withCharset("utf-8"), Map.of("Accept", GEOJSON.toString()));

        putEntityBody(requestContext, processedBody);

      } catch (Throwable e) {
        // ignore
        boolean stop = true;
      }
    }
  }

  private PolicyDecision askPDP(String user, String method, String path, byte[] body) {

    LOGGER.debug("EDA {} {} {} {}", user, method, path, new String(body, Charset.forName("utf-8")));

    try {
      byte[] xacmlRequest = getXacmlRequest(user, method, path, body);

      InputStream response =
          httpClient.postAsInputStream(
              edaUrl, xacmlRequest, mediaType, Map.of("Accept", mediaTypeAccept.toString()));

      XacmlResponse xacmlResponse = getXacmlResponse(response);

      return xacmlResponse.isAllowed()
          ? PolicyDecision.PERMIT
          : xacmlResponse.isNotApplicable() ? PolicyDecision.NOT_APPLICABLE : PolicyDecision.DENY;

    } catch (Throwable e) {
      // ignore
      LogContext.errorAsDebug(LOGGER, e, "Error requesting a policy decision");
    }

    return PolicyDecision.DENY;
  }

  // TODO
  private byte[] getXacmlRequest(String user, String method, String path, byte[] body)
      throws JsonProcessingException {
    Object xacmlRequest = null;
    /*xacmlJson10
    ? new XacmlRequest(
        Version._1_0,
        Optional.empty(),
        method,
        method,
        "",
        path,
        Optional.ofNullable(body),
        Map.of())
    : new XacmlRequest(
        Version._1_1,
        Optional.empty(),
        method,
        method,
        "",
        path,
        Optional.ofNullable(body),
        Map.of());*/

    LOGGER.debug(
        "XACML {}", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlRequest));

    return JSON.writeValueAsBytes(xacmlRequest);
  }

  private XacmlResponse getXacmlResponse(InputStream response) throws IOException {
    XacmlResponse xacmlResponse = JSON.readValue(response, XacmlResponse.class);

    LOGGER.debug(
        "XACML R {}", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlResponse));

    return xacmlResponse;
  }

  private byte[] getEntityBody(ContainerRequestContext requestContext) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = requestContext.getEntityStream();

    // final StringBuilder b = new StringBuilder();
    try {
      ReaderWriter.writeTo(in, out);

      byte[] requestEntity = out.toByteArray();
      /*if (requestEntity.length == 0) {
          b.append("")
           .append("\n");
      } else {
          b.append(new String(requestEntity))
           .append("\n");
      }*/
      requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));

      return requestEntity;

    } catch (IOException ex) {
      // Handle logging error
    }
    return new byte[0];
  }

  private void putEntityBody(ContainerRequestContext requestContext, InputStream body) {
    requestContext.setEntityStream(body);
  }
}
