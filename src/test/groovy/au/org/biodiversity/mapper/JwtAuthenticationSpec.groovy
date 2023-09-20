package au.org.biodiversity.mapper

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.rxjava3.http.client.Rx3HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.render.BearerAccessRefreshToken
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class JwtAuthenticationSpec extends Specification {

    @Inject
    @Client('/')
    Rx3HttpClient client
    @Inject ApiController controller

    void "Check if accessToken exists in the response from /login endpoint"() {
        when:
        UsernamePasswordCredentials adminUserCreds = new UsernamePasswordCredentials("TEST-services", "buy-me-a-pony")
        HttpRequest loginRequest = HttpRequest.POST('/api/login', adminUserCreds)
        HttpResponse<BearerAccessRefreshToken> response = client.toBlocking().exchange(loginRequest, BearerAccessRefreshToken)
        BearerAccessRefreshToken token = response?.body()

        then :
        response.status == HttpStatus.OK
        token.accessToken
        token.username == 'TEST-services'
    }

    void "Make a successful request to secured endpoint"() {
        when:
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("TEST-services", "buy-me-a-pony")
        HttpRequest request = HttpRequest.POST('/api/login', creds)
        HttpResponse response = client.toBlocking().exchange(request, BearerAccessRefreshToken)
        String accessToken = response.body().accessToken
        String refreshToken = response.body().refreshToken
        println "Ref Tok: $refreshToken"

        then:
        JWTParser.parse(accessToken) instanceof SignedJWT

        when: "Gen a new access token using refresh token"
        String data = '{"grant_type":"refresh_token", "refresh_token": "' + refreshToken + '"}'
        HttpRequest reqToOauthAccessToken = HttpRequest
                .POST('/api/oauth/access_token', data)
                .contentType(MediaType.APPLICATION_JSON)
        println "Data: $data"
        HttpResponse<BearerAccessRefreshToken> refreshRes =
                client
                        .toBlocking()
                        .retrieve(reqToOauthAccessToken, BearerAccessRefreshToken) as HttpResponse<BearerAccessRefreshToken>

        then:
        println "Refresh Response: $refreshRes.properties"
        refreshRes
    }
}
