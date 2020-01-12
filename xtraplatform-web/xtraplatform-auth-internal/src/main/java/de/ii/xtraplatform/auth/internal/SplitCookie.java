package de.ii.xtraplatform.auth.internal;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SplitCookie {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitCookieCredentialAuthFilter.class);

    private static final String TOKEN_COOKIE_NAME = "xtraplatform-token";
    private static final String SIGNATURE_COOKIE_NAME = "xtraplatform-signature";

    static Optional<String> readToken(Map<String, Cookie> cookies) {
        Optional<Cookie> tokenCookie = Optional.ofNullable(cookies.get(TOKEN_COOKIE_NAME));
        Optional<Cookie> signatureCookie = Optional.ofNullable(cookies.get(SIGNATURE_COOKIE_NAME));

        if (tokenCookie.isPresent() && signatureCookie.isPresent()) {
            return Optional.ofNullable(String.format("%s.%s", tokenCookie.get()
                                                                         .getValue(), signatureCookie.get()
                                                                                                     .getValue()));
        }

        return Optional.empty();
    }

    public static List<String> writeToken(String token, String domain, boolean secure, boolean rememberMe) {

        int lastDot = token.lastIndexOf(".");
        String headerPayload = token.substring(0, lastDot);
        String signature = token.substring(lastDot + 1);

        // with rememberMe a user will be logged out after 30 days of inactivity
        // without rememberMe a user will be logged out after 1 hour of inactivity or a session end
        int payloadExpires = rememberMe ? 2592000 : 3600;
        int signatureExpires = rememberMe ? 2592000 : -1;

        return ImmutableList.of(
                getPayloadCookie(headerPayload, domain, secure, payloadExpires),
                getSignatureCookie(signature, domain, secure, signatureExpires)
        );
    }

    static private String getPayloadCookie(String payload, String domain, boolean secure, int expires) {
        NewCookie newCookie = new NewCookie(TOKEN_COOKIE_NAME, payload, "/", null, "", expires, secure, false);

        return String.format("%s;SameSite=strict", newCookie);
    }

    static private String getSignatureCookie(String signature, String domain, boolean secure, int expires) {
        NewCookie newCookie = new NewCookie(SIGNATURE_COOKIE_NAME, signature, "/", null, "", expires, secure, true);

        return String.format("%s;SameSite=strict", newCookie);
    }
}
