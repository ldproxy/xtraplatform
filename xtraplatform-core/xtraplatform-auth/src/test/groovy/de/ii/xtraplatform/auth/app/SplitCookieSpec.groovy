package de.ii.xtraplatform.auth.app

import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie

class SplitCookieSpec extends Specification {

    @Shared Cookie payloadCookie = new NewCookie("xtraplatform-token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0")
    @Shared Cookie signatureCookie = new NewCookie("xtraplatform-signature", "iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc")

    def 'Test read token'() {
        given:
        Map<String, Cookie> cookies = ["xtraplatform-token":payloadCookie, "xtraplatform-signature":signatureCookie]

        when:
        Optional<String> token = SplitCookie.readToken(cookies)

        then:
        token.isPresent()
        token.get() == "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0.iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc"
    }

    def 'Test read token on different inputs'() {
        when:
        Optional<String> token = SplitCookie.readToken(cookies)

        then:
        token.isEmpty()

        where:
        cookies                                                                                 | _
        ["xtraplatform-token":null, "xtraplatform-signature":null]                              | _
        ["xtraplatform-token":payloadCookie]                                                    | _
        ["xtraplatform-signature":signatureCookie]                                              | _
        ["xtraplatform":payloadCookie]                                                          | _
    }

    def 'Test writeToken'() {
        given:
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0.iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc"

        when:
        List<String> result = SplitCookie.writeToken(testToken, "localhost", true, true)

        then:
        result.size() == 2
        String payloadCookie = result.get(0)
        payloadCookie.contains("xtraplatform-token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0")
        payloadCookie.contains(";Secure;")
        payloadCookie.contains(";Max-Age=2592000;")
        payloadCookie.endsWith(";SameSite=strict")
        String signatureCookie = result.get(1)
        signatureCookie.contains("xtraplatform-signature=iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc")
        signatureCookie.contains(";Secure;")
        signatureCookie.contains(";Max-Age=2592000;")
        signatureCookie.endsWith(";SameSite=strict")
    }
}
