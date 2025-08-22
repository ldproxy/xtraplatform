package de.ii.xtraplatform.auth.app

import de.ii.xtraplatform.auth.domain.ImmutableUser
import de.ii.xtraplatform.auth.domain.Oidc
import de.ii.xtraplatform.auth.domain.Role
import de.ii.xtraplatform.auth.domain.User
import de.ii.xtraplatform.base.domain.*
import de.ii.xtraplatform.blobs.domain.ResourceStore
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Use https://www.jwt.io to encode or decode tokens
 */
class JwtTokenHandlerSpec extends Specification {

    @Shared
    JwtTokenHandler jwtTokenHandler
    @Shared
    byte[] secretKey = "bWjviO9eI9/i/ft15U49dbqAJiGyuHAXcP1na2QZZyo=".getBytes()

    def setupSpec() {
        def auth = ModifiableAuthConfiguration.create()
        auth.putProviders("jwt", new ImmutableJwt.Builder()
                .type(AuthConfiguration.AuthProviderType.JWT)
                .signingKey(Base64.getEncoder().encodeToString(secretKey))
                .claims(new ImmutableClaims.Builder()
                        .permissions("resource_access.{{apiId}}.roles")
                        .build()
                )
                .build()
        )

        AppConfiguration config = Stub(AppConfiguration) {
            getAuth() >> auth
        }
        AppContext ac = Stub(AppContext) {
            getConfiguration() >> config
        }
        Oidc oidc = Stub(Oidc) {
            isEnabled() >> false
        }

        jwtTokenHandler = new JwtTokenHandler(ac, Stub(ResourceStore), oidc, null)
        jwtTokenHandler.onStart(false)
    }

    @Ignore
    //TODO
    def 'Test token generation'() {
        given:
        ImmutableUser user = ImmutableUser.builder().name("foobar").role(Role.ADMIN).build()
        int expiresIn = 60

        when:
        String token = jwtTokenHandler.generateToken(user, expiresIn, true)
        Long timestamp = (Long) (System.currentTimeMillis() / 1000L) + 3600

        then:
        Objects.nonNull(token)
        String[] tokenParts = token.split("\\.")
        tokenParts.size() == 3
        decodeBase64Url(tokenParts[0]) == "{\"alg\":\"HS256\"}" // header
        decodeBase64Url(tokenParts[1]) == String.format("{\"sub\":\"foobar\",\"role\":\"ADMIN\",\"rememberMe\":true,\"exp\":%d}", timestamp) // payload
        tokenParts[2] == getExpectedSignature(tokenParts[0], tokenParts[1]) // signature

    }


    def 'Test token parsing'() {
        given:
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb29iYXIiLCJyb2xlIjoiQURNSU4iLCJyZW1lbWJlck1lIjp0cnVlLCJleHAiOjIxMDAwMDAwMDAsImF1ZCI6ImxkcHJveHkiLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwicm9sZXMiOlsiZGF0YTpyZWFkIiwiZGF0YTp3cml0ZSJdfQ.9bmFBXYOr7WJs8rAsDf5QT9uPIRNi5ofKzP6vd89i5w"

        when:
        Optional<User> user = jwtTokenHandler.parseToken(token)

        then:
        user.isPresent()
        user.get().getName() == "foobar"
        user.get().getRole() == Role.ADMIN
        !user.get().getForceChangePassword()
        user.get().getAudience() == ["ldproxy"] as Set
        user.get().getScopes() == ["read", "write"] as Set
        user.get().getPermissions() == [] as Set
        user.get().getPermissions("myapi") == [] as Set
    }


    def 'Test token permissions parsing'() {
        given:
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb29iYXIiLCJyb2xlIjoiQURNSU4iLCJyZW1lbWJlck1lIjp0cnVlLCJleHAiOjIxMDAwMDAwMDAsImF1ZCI6ImxkcHJveHkiLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwicmVzb3VyY2VfYWNjZXNzIjp7Int7YXBpSWR9fSI6eyJyb2xlcyI6WyJkYXRhOnJlYWQiXX0sIm15YXBpIjp7InJvbGVzIjpbImRhdGE6cmVhZCIsImRhdGE6d3JpdGUiXX19fQ.8Cea4J_x1ei7MgYJFjnXTzuyqdWXCkzogx4VeA42MyA"

        when:
        Optional<User> user = jwtTokenHandler.parseToken(token)

        then:
        user.isPresent()
        user.get().getPermissions() == ["data:read"] as Set
        user.get().getPermissions("myapi") == ["data:read", "data:write"] as Set
    }

    def 'Test token parsing on incorrect inputs'() {
        when:
        Optional<User> user = jwtTokenHandler.parseToken(token)

        then:
        user.isEmpty()

        where:
        token                                                                                                                                                                       | _
        null                                                                                                                                                                        | _
        ""                                                                                                                                                                          | _
        // incomplete token:
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"                                                                                                                                      | _
        // expired JWT token:
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb29iYXIiLCJyb2xlIjoiQURNSU4iLCJyZW1lbWJlck1lIjp0cnVlLCJleHAiOjE2MTA2MzI4MDB9.7GwogdWyig1CeyiiYs-4gLOOPbPVAd95k1ifN7qY3PE" | _
        // token with a signature generated using a different secret key
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb29iYXIiLCJyb2xlIjoiQURNSU4iLCJyZW1lbWJlck1lIjp0cnVlLCJleHAiOjIxMDAwMDAwMDB9.TlQwANh4_q-1r9wndHyLOZLp4Lua9HQlLrzNZxzsJb4" | _
    }

    def 'Test token claim parsing'() {
        given:
        String tokenWithCustomClaims = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb29iYXIiLCJyb2xlIjoiQURNSU4iLCJyZW1lbWJlck1lIjp0cnVlLCJleHAiOjIxMDAwMDAwMDAsInRlc3RDbGFpbUJvb2wiOnRydWUsInRlc3RDbGFpbVN0cmluZyI6ImZvbyIsInRlc3RDbGFpbUludCI6MTIzNzg5fQ.ocOKAl8t3FJDst2abGMsFpHUBHRKL_Qj4b42v8qmX-s"

        when:
        Optional<Boolean> customClaimBoolean = jwtTokenHandler.parseTokenClaim(tokenWithCustomClaims, "testClaimBool", Boolean.class)
        Optional<String> customClaimString = jwtTokenHandler.parseTokenClaim(tokenWithCustomClaims, "testClaimString", String.class)
        Optional<Integer> customClaimInteger = jwtTokenHandler.parseTokenClaim(tokenWithCustomClaims, "testClaimInt", Integer.class)

        then:
        customClaimBoolean.isPresent()
        customClaimBoolean.get()
        customClaimString.isPresent()
        customClaimString.get() == "foo"
        customClaimInteger.isPresent()
        customClaimInteger.get() == Integer.valueOf(123789)
    }


    String getExpectedSignature(header, payload) {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256")
        sha256_HMAC.init(new SecretKeySpec(secretKey, "HmacSHA256"))
        byte[] result = sha256_HMAC.doFinal(String.format("%s.%s", header, payload).getBytes())
        return new String(Base64.getUrlEncoder().withoutPadding().encode(result))
    }

    String decodeBase64Url(String input) {
        return new String(Base64.getUrlDecoder().decode(input))
    }
}
