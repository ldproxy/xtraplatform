/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external

import de.ii.xtraplatform.base.domain.AuthConfiguration
import de.ii.xtraplatform.base.domain.AuthConfiguration.Jwt
import de.ii.xtraplatform.base.domain.AuthConfiguration.UserInfo
import de.ii.xtraplatform.base.domain.ImmutableClaims
import de.ii.xtraplatform.web.domain.HttpClient
import spock.lang.Specification

class TokenAuthenticatorSpec extends Specification {

    def "the token is URL-encoded when interpolated into the user-info endpoint URL"() {
        given:
        def claims = new ImmutableClaims.Builder().build()
        UserInfo userInfo = Stub() {
            getEndpoint() >> "https://idp.example/userinfo?token={token}"
            getClaims() >> claims
        }
        Jwt jwt = Stub() {
            getClaims() >> claims
        }
        AuthConfiguration authConfig = Stub() {
            getUserInfo() >> Optional.of(userInfo)
            getJwt() >> Optional.of(jwt)
        }
        String capturedUrl = null
        HttpClient httpClient = Mock() {
            getAsInputStream(_, _) >> { args ->
                capturedUrl = args[0]
                new ByteArrayInputStream("{}".getBytes("UTF-8"))
            }
        }
        def authenticator = new TokenAuthenticator(authConfig, httpClient)

        when:
        authenticator.authenticate("a b/c?d=e")

        then: "URL metacharacters in the token are percent-encoded, not injected into the URL"
        capturedUrl == "https://idp.example/userinfo?token=a+b%2Fc%3Fd%3De"
    }
}
