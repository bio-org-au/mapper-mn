package au.org.biodiversity.mapper

import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import org.junit.Ignore
import spock.lang.Specification
import javax.inject.Inject

import static io.micronaut.http.HttpRequest.DELETE
import static io.micronaut.http.HttpRequest.GET
import static io.micronaut.http.HttpRequest.PUT
import static io.micronaut.http.HttpRequest.POST

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

    @Inject
    @Client(value = "/api/", configuration = TestHttpClientConfiguration.class)
    HttpClient client

    void "test getting preferred host"() {
        when: "I ask for pref host"
        Map resp = httpCallMap('preferred-host')

        then:
        resp.host == 'http://localhost:8080'
    }

    void "test getting preferred link"() {
        when: "I ask for preferred link"
        Map resp = httpCallMap('preferred-link/name/apni/54433')

        then:
        resp.link == 'http://localhost:8080/name/apni/54433'

        when: "I ask for preferred link that doesn't exist"
        resp = httpCallMap('preferred-link/name/apni/99999')

        then:
        HttpClientResponseException notFound = thrown()
        notFound.message == "Page Not Found"
    }

    void "test getting links"() {
        when: "I ask for links"
        List<Map> resp = httpCallList('links/name/apni/54433')

        then:
        resp.size() == 2
        resp[0].link == 'http://localhost:8080/name/apni/54433'
        resp[0].resourceCount == 1
        resp[0].preferred == true


        when: "I ask for preferred link that doesn't exist"
        resp = httpCallList('links/name/apni/99999')

        then:
        HttpClientResponseException notFound = thrown()
        notFound.message == "Page Not Found"
    }

    void "test getting identity for a uri"() {
        when: "I ask for the identity"
        String encodedUrl = URLEncoder.encode('http://localhost:8080/name/apni/54433', 'UTF-8')
        List<Map> resp = httpCallList('/current-identity?uri=' + encodedUrl)

        then: "We expect a list of identities"
        resp.size() == 1
        resp[0].objectType == 'name'
        resp[0].nameSpace == 'apni'
        resp[0].idNumber == 54433
        resp[0].versionNumber == 0

        when: "I ask for preferred link that doesn't exist"
        httpCallList('current-identity?uri=http://localhost:8080/name/apni/99999')

        then:
        HttpClientResponseException notFound = thrown()
        notFound.message == "Page Not Found"

        when: "I ask for a match that has no identities, but exists"
        encodedUrl = URLEncoder.encode('http://localhost:8080/no-identifier/match', 'UTF-8')
        resp = httpCallList('/current-identity?uri=' + encodedUrl)

        then: "We expect a list of no identities"
        resp.size() == 0
    }

    void "test add identifier"() {
        when: "I put an identifier"
        //    @Put("/add-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
        UriBuilder uri = UriBuilder.of('/add-identifier')
        uri.queryParam('nameSpace', nameSpace)
        uri.queryParam('objectType', objectType)
        uri.queryParam('idNumber', idNumber)
        uri.queryParam('versionNumber', versionNumber)
        uri.queryParam('uri', setUri)

        Map resp = httpPutCallMap(uri.toString(), [:])

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
    }

    void "test new add (identifier)"() {
        //    @Put("/add/{objectType}/{nameSpace}/{idNumber}")
        when: "I put an identifier"
        String uri = "/add/$objectType/$nameSpace/$idNumber"
        Map resp = httpPutCallMap(uri, [uri: setUri])

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
        //     @Put("/add/{nameSpace}/{objectType}/{versionNumber}/{idNumber}")
        when: "I put an identifier"
        String uri = "/add/$nameSpace/$objectType/$versionNumber/$idNumber"
        Map resp = httpPutCallMap(uri, [uri: setUri])

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
        when:
        Map r1 = httpPutCallMap('/add-host', [hostName: 'mcneils.net'])

        then:
        r1
        r1.host
        r1.host.hostName == 'mcneils.net'
        r1.host.preferred == false

        when: "I add it again I get back the same host"
        Map r2 = httpPutCallMap('/add-host', [hostName: 'mcneils.net'])

        then:
        r2
        r2.host.id == r1.host.id
        r2.host.hostName == r1.host.hostName

        when: "I set it as preferred"
        Map r3 = httpPutCallMap('/set-preferred-host', [hostName: 'mcneils.net'])

        then: "it is set"
        r3
        r3.host.preferred
        r3.host.id == r2.host.id

        when: "I set a non existent host as preferred"
        Map r4 = httpPutCallMap('/set-preferred-host', [hostName: 'neils.net'])

        then: "not found exception"
        HttpClientResponseException notFound = thrown()
        notFound.message == "Page Not Found"

        cleanup:
        httpPutCallMap('/set-preferred-host', [hostName: 'localhost:8080'])
    }

    void "test bulk add"() {
        when:
        Set<Map> bulkTreeIds = TestHelpers.getBulkTreeIds()
        Map r1 = httpPostCallMap('/bulk-add-identifiers', [identifiers: bulkTreeIds])

        then:
        r1
        r1.success
        r1.message == '36040 identities added.'

        when: "I remove them"
        Map r2 = httpPostCallMap('/bulk-remove-identifiers', [identifiers: bulkTreeIds])
        Thread.sleep(10000) //wait for the orphans to be removed

        then:
        r2
        r2.success
        r2.message == '36040 identities removed.'
        mappingService.stats().matches < 20

    }

    void "test add uri to identifier"() {
        when: "When I add a new URI to an existing identifier"
        UriBuilder uri = UriBuilder.of('/add-uri-to-identifier')
        uri.queryParam('nameSpace', 'apni')
        uri.queryParam('objectType', 'name')
        uri.queryParam('idNumber', 54433)
        uri.queryParam('uri', '54433/apni/name')
        Map r1 = httpPutCallMap(uri.toString(), [:])

        then: "It all succeeds"
        r1
        r1.success
        r1.message
        r1.match.uri == '54433/apni/name'
        r1.identifier.idNumber == 54433

        when: "When I try and add a URI to a non existant identifier"
        UriBuilder uri2 = UriBuilder.of('/add-uri-to-identifier')
        uri2.queryParam('nameSpace', 'apni')
        uri2.queryParam('objectType', 'name')
        uri2.queryParam('idNumber', 0)
        uri2.queryParam('uri', 'this/wont/work')
        httpPutCallMap(uri2.toString(), [:])

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
        Map r1 = httpPutCallMap("/add/dog/animals/23", [uri: null])
        Map r2 = httpPutCallMap("/add/dog/animals/24", [uri: null])
        List<Map> l1 = mappingService.getlinks('animals', 'dog', 23)
        List<Map> l2 = mappingService.getlinks('animals', 'dog', 24)

        expect:
        r1
        r2
        l1.size() == 1
        l2.size() == 1

        when: "I try to move"
        Map r3 = httpPostCallMap('/move-identity',
                [fromNameSpace: 'animals', fromObjectType: 'dog', fromIdNumber: 24,
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 23])
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
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 23])

        then: "get a not found"
        HttpClientResponseException notFound = thrown()
        notFound.message == "From identifier doesn't exist."

        when: "move an identity that doesn't exist"
        httpPostCallMap('/move-identity',
                [fromNameSpace: 'animals', fromObjectType: 'dog', fromIdNumber: 24,
                 toNameSpace  : 'animals', toObjectType: 'dog', toIdNumber: 25])

        then: "get a not found"
        HttpClientResponseException notFound2 = thrown()
        notFound2.message == "To identifier doesn't exist."

    }

    void "test remove Identity from uri"() {
        given:
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
        Map r1 = httpDeleteCallMap(uri.toString(), [:])

        then: "It all succeeds"
        r1
        r1.success
        r1.message == 'Identifier removed from URI.'
        r1.identitifier
        mappingService.getlinks('animals', 'dog', 1).size() == 1
        mappingService.stats().orphanMatch == orphans + 1

        when: "When I remove wrong URI from an existing identifier"
        UriBuilder u2 = UriBuilder.of('/remove-identifier-from-uri')
        u2.queryParam('nameSpace', 'animals')
        u2.queryParam('objectType', 'dog')
        u2.queryParam('idNumber', 1)
        u2.queryParam('uri', 'doggies/3')
        httpDeleteCallMap(u2.toString(), [:])

        then: "get a not found"
        HttpClientResponseException notFound = thrown()
        notFound.message == "URI doggies/3 doesn't exist."

        when: "When I remove from a non-existant identifier"
        UriBuilder u3 = UriBuilder.of('/remove-identifier-from-uri')
        u3.queryParam('nameSpace', 'animals')
        u3.queryParam('objectType', 'dog')
        u3.queryParam('idNumber', 4)
        u3.queryParam('uri', 'doggies/1')
        httpDeleteCallMap(u3.toString(), [:])

        then: "get a not found"
        HttpClientResponseException notFound2 = thrown()
        notFound2.message == "Identifier doesn't exist."
    }

    //*** helpers ***
    private Map httpPostCallMap(String uri, Map body) {
        Flowable<HttpResponse<Map>> call = client.exchange(
                POST(uri, body), Map.class
        )
        return call.blockingFirst().body()
    }

    private Map httpDeleteCallMap(String uri, Map body) {
        Flowable<Map> call = client.retrieve(
                DELETE(uri, body), Map.class
        )
        return call.blockingFirst()
    }

    private Map httpPutCallMap(String uri, Map body) {
        Flowable<Map> call = client.retrieve(
                PUT(uri, body), Map.class
        )
        return call.blockingFirst()
    }

    private Map httpCallMap(String uri) {
        Flowable<Map> call = client.retrieve(
                GET(uri), Map.class
        )
        return call.blockingFirst()
    }

    private List<Map> httpCallList(String uri) {
        Flowable<List<Map>> call = client.retrieve(
                GET(uri), List.class
        )
        return call.blockingFirst()
    }

}
