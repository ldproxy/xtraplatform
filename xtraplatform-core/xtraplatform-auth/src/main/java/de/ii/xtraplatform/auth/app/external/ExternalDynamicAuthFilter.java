/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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

  private static final MediaType XACML = new MediaType("application", "xacml+json", "utf-8");
  private static final MediaType GEOJSON = new MediaType("application", "geo+json", "utf-8");
  private static final ObjectMapper JSON =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final String edaUrl;
  private final String ppUrl;
  private final HttpClient httpClient;
  private final OAuthCredentialAuthFilter<P> delegate;

  ExternalDynamicAuthFilter(
      String edaUrl, String ppUrl, HttpClient httpClient, OAuthCredentialAuthFilter<P> delegate) {
    super();
    this.realm = "ldproxy";
    this.prefix = "Bearer";
    this.unauthorizedHandler = new DefaultUnauthorizedHandler();

    this.edaUrl = edaUrl;
    this.ppUrl = ppUrl;
    this.httpClient = httpClient;
    this.delegate = delegate;
  }

  // TODO
  static List<String> METHODS = ImmutableList.of("GET", "POST", "PUT", "DELETE");

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    SecurityContext oldSecurityContext = requestContext.getSecurityContext();

    delegate.filter(requestContext);

    if (METHODS.contains(requestContext.getMethod())) {

      List<String> pathSegments =
          Splitter.on('/').omitEmptyStrings().splitToList(requestContext.getUriInfo().getPath());
      int serviceIndex = pathSegments.indexOf("services");

      if (serviceIndex >= 0 && pathSegments.size() > serviceIndex) {
        boolean authorized =
            isAuthorized(
                requestContext.getSecurityContext().getUserPrincipal().getName(),
                requestContext.getMethod(),
                "/"
                    + Joiner.on('/')
                        .join(pathSegments.subList(serviceIndex + 1, pathSegments.size())),
                getEntityBody(requestContext));

        if (!authorized) {
          // reset security context, because we use @PermitAll and then decide based on Principal
          // existence
          requestContext.setSecurityContext(oldSecurityContext);
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

        InputStream processedBody = httpClient.postAsInputStream(ppUrl, body, GEOJSON);

        putEntityBody(requestContext, processedBody);

      } catch (Throwable e) {
        // ignore
        boolean stop = true;
      }
    }
  }

  private boolean isAuthorized(String user, String method, String path, byte[] body) {

    LOGGER.debug("EDA {} {} {} {}", user, method, path, new String(body, Charset.forName("utf-8")));

    try {

      XacmlRequest xacmlRequest1 = new XacmlRequest(user, method, path, body);
      byte[] xacmlRequest = JSON.writeValueAsBytes(xacmlRequest1);

      LOGGER.debug(
          "XACML {}", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlRequest1));

      InputStream response =
          httpClient.postAsInputStream(edaUrl, xacmlRequest, MediaType.APPLICATION_JSON_TYPE);

      XacmlResponse xacmlResponse = JSON.readValue(response, XacmlResponse.class);

      LOGGER.debug(
          "XACML R {}", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(xacmlResponse));

      return xacmlResponse.isAllowed() || xacmlResponse.isNotApplicable();

    } catch (Throwable e) {
      // ignore
    }

    return false;
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
