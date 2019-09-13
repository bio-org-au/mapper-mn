package au.org.biodiversity.mapper


import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Specification

import javax.inject.Inject

import static io.micronaut.http.HttpRequest.GET

/**
 * User: pmcneil
 * Date: 10/9/19
 *
 */
@MicronautTest
class ApiControllerSpec extends Specification {

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

    void "test getting identity for a uri"(){
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
