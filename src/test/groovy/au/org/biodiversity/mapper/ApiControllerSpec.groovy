/*
    Copyright (c) 2019 Australian National Botanic Gardens and Authors

    This file is part of National Species List project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package au.org.biodiversity.mapper

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.token.jwt.endpoints.TokenRefreshRequest
import io.micronaut.security.token.jwt.render.AccessRefreshToken
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject

import static io.micronaut.http.HttpRequest.*

/**
 * User: pmcneil
 * Date: 10/9/19
 *
 */
@MicronautTest
class ApiControllerSpec extends Specification {

    @Inject
    MappingService mappingService

    @Inject
    EmbeddedServer embeddedServer

    @Shared
    @AutoCleanup
    @Inject
    @Client(value = "/api/", configuration = TestHttpClientConfiguration.class)
    HttpClient client

    void "test auth"() {
        when:
        HttpRequest request = POST('/login', '{"username":"TEST-services","password":"buy-me-a-pony"}')
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken)


        then:
        rsp.status == HttpStatus.OK

        when:
        sleep(1000)
        String refreshToken = rsp.body().refreshToken
        String accessToken = rsp.body().accessToken

        HttpResponse<AccessRefreshToken> response = client.toBlocking().exchange(
                POST('/oauth/access_token', new TokenRefreshRequest("refresh_token", refreshToken)),
                AccessRefreshToken)

        then:
        response.status == HttpStatus.OK
        response.body().accessToken
        response.body().accessToken != accessToken
    }

    void "test getting preferred host"() {
        when: "I ask for pref host"
        Map resp = httpCallMap('preferred-host', null)

        then:
        resp.host == 'http://localhost:8080'
    }

    void "test getting preferred link"() {
        when: "I ask for preferred link"
        Map resp = httpCallMap('preferred-link/name/apni/54433', null)

        then:
        resp.link == 'http://localhost:8080/name/apni/54433'

        when: "I ask for preferred link that doesn't exist"
        Map res = httpCallMap('preferred-link/name/apni/99999', null)

        then:
        thrown HttpClientResponseException
        res == null
    }

    void "test getting links"() {
        when: "I ask for links"
        List<Map> resp = httpCallList('links/name/apni/54433', null)

        then:
        resp.size() == 2
        resp[0].link == 'http://localhost:8080/name/apni/54433'
        resp[0].resourceCount == 1
        resp[0].preferred == true


        when: "I ask for preferred link that doesn't exist"
        httpCallList('links/name/apni/99999', null)

        then:
        thrown HttpClientResponseException
    }

    void "test getting identity for a uri"() {
        when: "I ask for the identity"
        String encodedUrl = URLEncoder.encode('http://localhost:8080/name/apni/54433', 'UTF-8')
        List<Map> resp = httpCallList('/current-identity?uri=' + encodedUrl, null)

        then: "We expect a list of identities"
        resp.size() == 1
        resp[0].objectType == 'name'
        resp[0].nameSpace == 'apni'
        resp[0].idNumber == 54433
        resp[0].versionNumber == 0

        when: "I ask for preferred link that doesn't exist"
        httpCallList('current-identity?uri=http://localhost:8080/name/apni/99999', null)

        then:
        thrown HttpClientResponseException

        when: "I ask for a match that has no identities, but exists"
        encodedUrl = URLEncoder.encode('http://localhost:8080/no-identifier/match', 'UTF-8')
        resp = httpCallList('/current-identity?uri=' + encodedUrl, null)

        then: "We expect a list of no identities"
        resp.size() == 0
    }

    @Unroll
    void "test add identifier"() {
        given:
        String token = login()

        expect:
        token

        when: "I put an identifier"
        //    @Put("/add-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
        UriBuilder uri = UriBuilder.of('/add-identifier')
        uri.queryParam('nameSpace', nameSpace)
        uri.queryParam('objectType', objectType)
        uri.queryParam('idNumber', idNumber)
        uri.queryParam('versionNumber', versionNumber)
        uri.queryParam('uri', setUri)

        Map resp = httpPutCallMap(uri.toString(), [:], token)

        then: "I get the identifier object back"
        resp
        resp.identifier
        resp.identifier.objectType == objectType
        resp.identifier.nameSpace == nameSpace
        resp.identifier.idNumber == idNumber
        resp.identifier.versionNumber == versionNumber
        resp.uri == prefUri

        where:
        nameSpace     | objectType    | idNumber | versionNumber | setUri           | prefUri
        'electronics' | 'timer'       | 555      | null          | null             | 'timer/electronics/555'
        'electronics' | 'timer'       | 556      | null          | 'dual-timer/556' | 'dual-timer/556'
        'apni'        | 'treeElement' | 111      | 222           | null             | 'treeElement/222/111'
        'apni'        | 'treeElement' | 222      | 222           | 'tree/222/222'   | 'tree/222/222'
        'apni'        | 'treeElement' | 222      | 222           | 'tree/222/222'   | 'tree/222/222'
    }

    void "test new add (identifier)"() {
        given:
        String token = login()

        expect:
        token

//    @Put("/add/{objectType}/{nameSpace}/{idNumber}")
        when: "I put an identifier"
        String uri = "/add/$objectType/$nameSpace/$idNumber"
        Map resp = httpPutCallMap(uri, [uri: setUri], token)

        then: "I get the identifier object back"
        resp
        resp.identifier
        resp.identifier.objectType == objectType
        resp.identifier.nameSpace == nameSpace
        resp.identifier.idNumber == idNumber
        resp.identifier.versionNumber == versionNumber
        resp.uri == prefUri

        where:
        nameSpace     | objectType | idNumber | versionNumber | setUri           | prefUri
        'electronics' | 'timer'    | 555      | null          | null             | 'timer/electronics/555'
        'electronics' | 'timer'    | 556      | null          | 'dual-timer/556' | 'dual-timer/556'
    }

    void "test new add versioned (identifier)"() {
        given:
        String token = login()

        expect:
        token

        //     @Put("/add/{nameSpace}/{objectType}/{versionNumber}/{idNumber}")
        when: "I put an identifier"
        String uri = "/add/$nameSpace/$objectType/$versionNumber/$idNumber"
        Map resp = httpPutCallMap(uri, [uri: setUri], token)

        then: "I get the identifier object back"
        resp
        resp.identifier
        resp.identifier.objectType == objectType
        resp.identifier.nameSpace == nameSpace
        resp.identifier.idNumber == idNumber
        resp.identifier.versionNumber == versionNumber
        resp.uri == prefUri

        where:
        nameSpace | objectType    | idNumber | versionNumber | setUri         | prefUri
        'apni'    | 'treeElement' | 111      | 222           | null           | 'treeElement/222/111'
        'apni'    | 'treeElement' | 222      | 222           | 'tree/222/222' | 'tree/222/222'
    }

    void "test add/set preferred host"() {
        given:
        String token = login()

        expect:
        token

        when:
        Map r1 = httpPutCallMap('/add-host', [hostName: 'mcneils.net'], token)

        then:
        r1
        r1.host
        r1.host.hostName == 'mcneils.net'
        r1.host.preferred == false

        when: "I add it again I get back the same host"
        Map r2 = httpPutCallMap('/add-host', [hostName: 'mcneils.net'], token)

        then:
        r2
        r2.host.id == r1.host.id
        r2.host.hostName == r1.host.hostName

        when: "I set it as preferred"
        Map r3 = httpPutCallMap('/set-preferred-host', [hostName: 'mcneils.net'], token)

        then: "it is set"
        r3
        r3.host.preferred
        r3.host.id == r2.host.id

        when: "I set a non existent host as preferred"
        httpPutCallMap('/set-preferred-host', [hostName: 'neils.net'], token)

        then: "not found exception"
        HttpClientResponseException notFound = thrown()
        notFound.message == "Page Not Found"

        cleanup:
        httpPutCallMap('/set-preferred-host', [hostName: 'localhost:8080'], token)
    }

    void "test bulk add"() {
        given:
        String token = login()

        expect:
        token

        when:
        Set<Map> bulkTreeIds = TestHelpers.getBulkTreeIds()
        Map r1 = httpPostCallMap('/bulk-add-identifiers', [identifiers: bulkTreeIds], token)

        then:
        r1
        r1.success
        r1.message == '36040 identities added.'

        when: "I remove them"
        Map r2 = httpPostCallMap('/bulk-remove-identifiers', [identifiers: bulkTreeIds], token)
        Thread.sleep(10000) //wait for the orphans to be removed

        then:
        r2
        r2.success
        r2.message == '36040 identities removed.'
        mappingService.stats().matches < 20

    }

    void "test add uri to identifier"() {
        given:
        String token = login()

        expect:
        token

        when: "When I add a new URI to an existing identifier"
        UriBuilder uri = UriBuilder.of('/add-uri-to-identifier')
        uri.queryParam('nameSpace', 'apni')
        uri.queryParam('objectType', 'name')
        uri.queryParam('idNumber', 54433)
        uri.queryParam('uri', '54433/apni/name')
        Map r1 = httpPutCallMap(uri.toString(), [:], token)

        then: "It all succeeds"
        r1
        r1.success
        r1.message
        r1.match.uri == '54433/apni/name'
        r1.match.updatedBy == 'TEST-services'
        r1.identifier.idNumber == 54433
        r1.identifier.updatedBy == 'pmcneil'

        when: "When I try and add a URI to a non existant identifier"
        UriBuilder uri2 = UriBuilder.of('/add-uri-to-identifier')
        uri2.queryParam('nameSpace', 'apni')
        uri2.queryParam('objectType', 'name')
        uri2.queryParam('idNumber', 0)
        uri2.queryParam('uri', 'this/wont/work')
        httpPutCallMap(uri2.toString(), [:], token)

        then:
        HttpClientResponseException notFound = thrown()
        notFound.message == 'Identifier not found'

        cleanup:
        Match match = new Match((Map) r1.match, '')
        Identifier identifier = new Identifier(mappingService, (Map) r1.identifier, '')
        mappingService.removeIdentityFromUri(match, identifier)
    }

    void "test move Identity"() {
        given:
        String token = login()
        Map r1 = httpPutCallMap("/add/dog/animals/23", [uri: null], token)
        Map r2 = httpPutCallMap("/add/dog/animals/24", [uri: null], token)
        List<Map> l1 = mappingService.getlinks('animals', 'dog', 23)
        List<Map> l2 = mappingService.getlinks('animals', 'dog', 24)

        expect:
        r1
        r1.identifier
        r1.identifier.updatedBy == 'TEST-services'
        r2
        r2.identifier
        r2.identifier.updatedBy == 'TEST-services'
        l1.size() == 1
        l2.size() == 1

        when: "I try to move"
        Map r3 = httpPostCallMap('/move-identity',
                [fromNameSpace: 'animals', fromObjectType: 'dog', fromIdNumber: 24,
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 23], token)
        l1 = mappingService.getlinks('animals', 'dog', 23)
        l2 = mappingService.getlinks('animals', 'dog', 24)
        println l1

        then: "It works"
        r3
        r3.success
        l1.size() == 2
        l2.size() == 0

        when: "move an identity that doesn't exist"
        httpPostCallMap('/move-identity',
                [fromNameSpace: 'animals', fromObjectType: 'dog', fromIdNumber: 25,
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 23], token)

        then: "get a not found"
        HttpClientResponseException notFound = thrown()
        notFound.message == "From identifier doesn't exist."

        when: "move an identity that doesn't exist"
        httpPostCallMap('/move-identity',
                [fromNameSpace: 'animals', fromObjectType: 'dog', fromIdNumber: 24,
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 25], token)

        then: "get a not found"
        HttpClientResponseException notFound2 = thrown()
        notFound2.message == "To identifier doesn't exist."

    }

    void "test remove Identity from uri"() {
        given:
        String token = login()
        Identifier identifier = mappingService.addIdentifier('animals', 'dog',
                1, null, null, 'fred')
        Match match = mappingService.addMatch('doggies/1', 'fred')
        mappingService.addUriToIdentifier(identifier, match, false)

        expect:
        identifier
        match
        mappingService.getlinks('animals', 'dog', 1).size() == 2

        when: "When I remove a URI from an existing identifier"
        Integer orphans = mappingService.stats().orphanMatch as Integer
        UriBuilder uri = UriBuilder.of('/remove-identifier-from-uri')
        uri.queryParam('nameSpace', 'animals')
        uri.queryParam('objectType', 'dog')
        uri.queryParam('idNumber', 1)
        uri.queryParam('uri', 'doggies/1')
        Map r1 = httpDeleteCallMap(uri.toString(), [:], token)

        then: "It all succeeds"
        r1
        r1.success
        r1.message == 'Identifier removed from URI.'
        r1.identifier
        mappingService.getlinks('animals', 'dog', 1).size() == 1
        mappingService.stats().orphanMatch == orphans + 1

        when: "When I remove wrong URI from an existing identifier"
        UriBuilder u2 = UriBuilder.of('/remove-identifier-from-uri')
        u2.queryParam('nameSpace', 'animals')
        u2.queryParam('objectType', 'dog')
        u2.queryParam('idNumber', 1)
        u2.queryParam('uri', 'doggies/3')
        httpDeleteCallMap(u2.toString(), [:], token)

        then: "get a not found"
        HttpClientResponseException notFound = thrown()
        notFound.message == "URI doggies/3 doesn't exist."

        when: "When I remove from a non-existant identifier"
        UriBuilder u3 = UriBuilder.of('/remove-identifier-from-uri')
        u3.queryParam('nameSpace', 'animals')
        u3.queryParam('objectType', 'dog')
        u3.queryParam('idNumber', 4)
        u3.queryParam('uri', 'doggies/1')
        httpDeleteCallMap(u3.toString(), [:], token)

        then: "get a not found"
        HttpClientResponseException notFound2 = thrown()
        notFound2.message == "Identifier doesn't exist."
    }

    void "test delete identifier"() {
        given:
        String token = login()
        Identifier identifier = mappingService.addIdentifier('animals', 'rat',
                1, null, null, 'fred')

        expect:
        identifier
        identifier.preferredUriID
        !identifier.deleted
        token

        when: "I delete an identifier"
        UriBuilder u1 = UriBuilder.of('/delete-identifier')
        u1.queryParam('nameSpace', 'animals')
        u1.queryParam('objectType', 'rat')
        u1.queryParam('idNumber', 1)
        u1.queryParam('reason', 'just for kicks')
        Map r1 = httpDeleteCallMap(u1.toString(), [:], token)

        then:
        r1
        r1.success
        r1.identifier
        r1.identifier.deleted
        r1.identifier.reasonDeleted == 'just for kicks'

        when: "I delete sans reason"
        UriBuilder u2 = UriBuilder.of('/delete-identifier')
        u2.queryParam('nameSpace', 'animals')
        u2.queryParam('objectType', 'rat')
        u2.queryParam('idNumber', 1)
        httpDeleteCallMap(u2.toString(), [:], token)

        then:
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST
        e.message == "Reason cannot be null or blank."

        when: "I delete non-existent ident "
        UriBuilder u3 = UriBuilder.of('/delete-identifier')
        u3.queryParam('nameSpace', 'animals')
        u3.queryParam('objectType', 'rat')
        u3.queryParam('idNumber', 2)
        httpDeleteCallMap(u3.toString(), [:], token)

        then:
        HttpClientResponseException e2 = thrown()
        e2.status == HttpStatus.NOT_FOUND
        e2.message == 'Identifier doesn\'t exist.'
    }

    //*** helpers ***

    private String login() {
        HttpRequest request = POST('/login', '{"username":"TEST-services","password":"buy-me-a-pony"}')
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken)
        assert rsp.status == HttpStatus.OK
        return rsp.body().accessToken
    }

    private Map httpPostCallMap(String uri, Map body, String accessToken) {
        Flowable<HttpResponse<Map>> call = client.exchange(
                POST(uri, body)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                , Map.class
        )
        return call.blockingFirst().body()
    }

    private Map httpDeleteCallMap(String uri, Map body, String accessToken) {
        Flowable<Map> call = client.retrieve(
                DELETE(uri, body)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                , Map.class
        )
        return call.blockingFirst()
    }

    private Map httpPutCallMap(String uri, Map body, String accessToken) {
        Flowable<Map> call = client.retrieve(
                PUT(uri, body)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                , Map.class
        )
        return call.blockingFirst()
    }

    private Map httpCallMap(String uri, String accessToken) {
        Flowable<Map> call = client.retrieve(
                GET(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                , Map.class
        )
        return call.blockingFirst()
    }

    private List<Map> httpCallList(String uri, String accessToken) {
        Flowable<List<Map>> call = client.retrieve(
                GET(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                , List.class
        )
        return call.blockingFirst()
    }
}
