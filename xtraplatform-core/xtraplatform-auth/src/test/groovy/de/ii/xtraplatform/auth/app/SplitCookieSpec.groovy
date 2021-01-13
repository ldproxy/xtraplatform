package de.ii.xtraplatform.auth.app

import spock.lang.Specification

import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie

class SplitCookieSpec extends Specification {

    def 'Test read token'() {
        given:
        Cookie payloadCookie = new NewCookie("xtraplatform-token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0")
        Cookie signatureCookie = new NewCookie("xtraplatform-signature", "iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc")
        Map<String, Cookie> cookies = ["xtraplatform-token":payloadCookie, "xtraplatform-signature":signatureCookie]

        when:
        Optional<String> token = SplitCookie.readToken(cookies)

        then:
        token.isPresent()
        token.get() == "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0.iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc"
    }

    def 'Test writeToken'() {
        given:
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0.iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc"

        when:
        List<String> result = SplitCookie.writeToken(testToken, "localhost", true, true)

        then:
        result.size() == 2
        String payloadCookie = result.get(0)
        payloadCookie.contains("Value=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0")
        payloadCookie.contains("Domain=localhost")
        payloadCookie.contains(";Secure;")
        String signatureCookie = result.get(1)
        signatureCookie.contains("Value=iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc")
        signatureCookie.contains("Value=iCkcZgw3CO7aySPaKZgak0m7DpwAkxKuQrMmNyHfppc")
        signatureCookie.contains("Domain=localhost")
        signatureCookie.contains(";Secure;")
    }
}
