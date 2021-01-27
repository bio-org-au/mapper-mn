package au.org.biodiversity.mapper

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class JwtAuthenticationSpec extends Specification {
    @Inject
    EmbeddedServer embeddedServer

    @Inject
    @Client(value = "/api", configuration = TestHttpClientConfiguration.class)
    RxHttpClient client

    void "On successful authentication, response has an access and a refresh token"() {
        when: 'Login with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("TEST-services", "buy-me-a-pony")
        HttpRequest request = HttpRequest.POST('/login', creds)
        BearerAccessRefreshToken rsp = client.toBlocking().retrieve(request, BearerAccessRefreshToken)

        then:
        rsp.username == 'TEST-services'
        rsp.accessToken
        rsp.refreshToken

        and: 'access token is a JWT'
        JWTParser.parse(rsp.accessToken) instanceof SignedJWT
    }

    void "Successful authentication returns Json Web token"() {
        when: 'Login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("TEST-services", "buy-me-a-pony")
        HttpRequest request = HttpRequest.POST('/login', creds)
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken)

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK

//        when:
//        BearerAccessRefreshToken bearerAccessRefreshToken = rsp.body()
//
//        then:
//        bearerAccessRefreshToken.username == 'TEST-services'
//        bearerAccessRefreshToken.accessToken
//
//        and: 'the access token is a signed JWT'
//        JWTParser.parse(bearerAccessRefreshToken.accessToken) instanceof SignedJWT
//
//        when: 'passing the access token as in the Authorization HTTP Header with the prefix Bearer allows the user to access a secured endpoint'
//        String accessToken = bearerAccessRefreshToken.accessToken
//        HttpRequest requestWithAuthorization = HttpRequest.GET('/' )
//                .accept(MediaType.TEXT_PLAIN)
//                .bearerAuth(accessToken)
//        HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String)
//
//        then:
//        response.status == HttpStatus.OK
//        response.body() == 'TEST-services'
    }
}
