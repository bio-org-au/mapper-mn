package au.org.biodiversity.mapper

import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.ApplicationConfiguration

import javax.inject.Named
import javax.inject.Singleton

import static io.micronaut.http.HttpRequest.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Specification

import javax.inject.Inject

/**
 * User: pmcneil
 * Date: 9/9/19
 *
 */
@MicronautTest
class BrokerControllerSpec extends Specification {
    @Inject
    EmbeddedServer embeddedServer

    @Inject
    @Client(value = "/broker/", configuration = TestHttpClientConfiguration.class)
    HttpClient client

    void "test broker redirection"() {
        when: "I ask for a valid current uri"
        HttpResponse<String> resp = httpCall('name/apni/54433')

        then: "I get a see other redirection (303)"
        resp.getStatus() == HttpStatus.SEE_OTHER

        when: "I ask for a deprecated uri"
        resp = httpCall('cgi-bin/apni?taxon_id=230687')

        then: "I get a moved permanently (301)"
        resp.getStatus() == HttpStatus.MOVED_PERMANENTLY

        when: "I ask for something that's not there"
        resp = httpCall('blah/blah/bang')

        then: "I get a not found (401)"
        HttpClientResponseException notFound = thrown()
        notFound.message == 'blah/blah/bang not found'

        when: "I ask for a deleted object"
        resp = httpCall('name/apni/148297')

        then: "I get a gone (410)"
        HttpClientResponseException gone = thrown()
        gone.message == 'Name has not been applied to Australian flora'

        when: "I get a deprecated match that doesn't have a preferred uri"
        resp = httpCall('name/blah/666')

        then: "I get a see other redirection to the resource (303)"
        resp.getStatus() == HttpStatus.SEE_OTHER

        when: "I ask for a resource that breaks"
        resp = httpCall('name/apni/54433.rdf')

        then: "I get an internal error (500)"
        HttpClientResponseException internalError = thrown()
        internalError.message == 'Service URL not found.'
    }

    private HttpResponse<String> httpCall(String uri){
        Flowable<HttpResponse<String>> call = client.exchange(
                GET(uri), String.class
        )
        return call.blockingFirst()
    }
}

