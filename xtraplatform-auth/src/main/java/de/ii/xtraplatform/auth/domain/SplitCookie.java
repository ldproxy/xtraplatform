/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

public final class SplitCookie {

  private static final String TOKEN_COOKIE_NAME = "xtraplatform-token";
  private static final String SIGNATURE_COOKIE_NAME = "xtraplatform-signature";

  private SplitCookie() {}

  public static Optional<String> readToken(Map<String, Cookie> cookies) {
    Optional<Cookie> tokenCookie = Optional.ofNullable(cookies.get(TOKEN_COOKIE_NAME));
    Optional<Cookie> signatureCookie = Optional.ofNullable(cookies.get(SIGNATURE_COOKIE_NAME));

    if (tokenCookie.isPresent() && signatureCookie.isPresent()) {
      return Optional.ofNullable(
          String.format("%s.%s", tokenCookie.get().getValue(), signatureCookie.get().getValue()));
    }

    return Optional.empty();
  }

  public static List<String> writeToken(
      String token, String domain, boolean secure, boolean rememberMe) {

    int lastDot = token.lastIndexOf('.');
    String headerPayload = token.substring(0, lastDot);
    String signature = token.substring(lastDot + 1);

    // with rememberMe a user will be logged out after 30 days of inactivity
    // without rememberMe a user will be logged out after 1 hour of inactivity or a session end
    int payloadExpires = rememberMe ? 2_592_000 : 3_600;
    int signatureExpires = rememberMe ? 2_592_000 : -1;

    return ImmutableList.of(
        getPayloadCookie(headerPayload, secure, payloadExpires),
        getSignatureCookie(signature, secure, signatureExpires));
  }

  public static List<String> deleteToken(String domain, boolean secure) {
    return ImmutableList.of(getPayloadCookie(null, secure, 0), getSignatureCookie(null, secure, 0));
  }

  private static String getPayloadCookie(String payload, boolean secure, int expires) {
    NewCookie newCookie =
        new NewCookie(TOKEN_COOKIE_NAME, payload, "/", null, "", expires, secure, false);

    return String.format("%s;SameSite=strict", newCookie);
  }

  private static String getSignatureCookie(String signature, boolean secure, int expires) {
    NewCookie newCookie =
        new NewCookie(SIGNATURE_COOKIE_NAME, signature, "/", null, "", expires, secure, true);

    return String.format("%s;SameSite=strict", newCookie);
  }
}
