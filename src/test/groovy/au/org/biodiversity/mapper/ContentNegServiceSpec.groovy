package au.org.biodiversity.mapper

import io.micronaut.http.MediaType
import io.micronaut.test.annotation.MicronautTest
import spock.lang.*
import javax.inject.Inject

/**
 * note you need to supply a config file for the contentNegService
 * e.g. -Dmicronaut.config.files=./src/test/resource/nsl-mapper-config-mn.groovy
 *
 * in intellij you can do this in the configuration for the test runner by placing that line in the VM options
 * field.
 *
 * User: pmcneil
 * Date: 6/9/19
 *
 */
@MicronautTest
class ContentNegServiceSpec extends Specification {

    @Inject
    ContentNegService contentNegService

    @Unroll
    void "choose content #desc test"() {
        when: "accept header asks for #accept"
        MediaType choosenOne = contentNegService.chooseContentType(accept, extension)

        then: "we get #result"
        choosenOne == result

        where:
        accept                                                                | extension | result                     | desc
        [MediaType.TEXT_HTML_TYPE]                                            | null      | MediaType.TEXT_HTML_TYPE   | 'picks first acceptible (HTML) in header list order'
        [MediaType.TEXT_HTML_TYPE, MediaType.TEXT_JSON_TYPE]                  | null      | MediaType.TEXT_HTML_TYPE   | 'picks first acceptible (HTML) in header list order'
        [MediaType.TEXT_JSON_TYPE, MediaType.TEXT_HTML_TYPE]                  | null      | MediaType.TEXT_JSON_TYPE   | 'picks first acceptible (JSON) in header list order'
        [MediaType.TEXT_XML_TYPE, MediaType.TEXT_HTML_TYPE]                   | null      | MediaType.TEXT_XML_TYPE    | 'picks first acceptible (XML) in header list order'
        [ContentNegService.RDF_TYPE, MediaType.TEXT_HTML_TYPE]                | null      | ContentNegService.RDF_TYPE | 'picks first acceptible (RDF)'
        [new MediaType('application/octet', 'oct'), MediaType.TEXT_JSON_TYPE] | null      | MediaType.TEXT_JSON_TYPE   | 'picks first acceptible with unacceptible type first'
        []                                                                    | '.html'   | MediaType.TEXT_HTML_TYPE   | 'picks extension (HTML) when no accepts'
        [MediaType.TEXT_HTML_TYPE]                                            | '.json'   | MediaType.TEXT_JSON_TYPE   | 'picks extension (JSON) over accepts'
        [MediaType.TEXT_HTML_TYPE]                                            | '.xml'    | MediaType.TEXT_XML_TYPE    | 'picks extension (XML) over accepts'
        [MediaType.TEXT_HTML_TYPE]                                            | '.rdf'    | ContentNegService.RDF_TYPE | 'picks extension (RDF) over accepts'
        [MediaType.TEXT_HTML_TYPE, MediaType.TEXT_JSON_TYPE]                  | '.fred'   | MediaType.TEXT_HTML_TYPE   | 'bad extension - picks first acceptible (HTML) in header list order'
        [MediaType.TEXT_JSON_TYPE, MediaType.TEXT_HTML_TYPE]                  | '.fred'   | MediaType.TEXT_JSON_TYPE   | 'bad extension - picks first acceptible (JSON) in header list order'
        [new MediaType('application/octet', 'oct'), MediaType.TEXT_JSON_TYPE] | '.fred'   | MediaType.TEXT_JSON_TYPE   | 'bad extension - picks first acceptible (JSON) with unacceptible type first'
    }

    @Unroll
    void "resolver test #mediatype #objectType #nameSpace"() {

        when:
        Identifier identifier = new Identifier(null, [i_name_space: nameSpace, i_object_type: objectType, i_id_number: 12345, i_version_number: version])
        String url = contentNegService.resolveServiceUrl(identifier, mediatype)

        then:
        url == expectedUrl

        where:
        mediatype             | nameSpace | objectType    | version | expectedUrl
        'text/html'           | 'apni'    | 'name'        | null    | 'http://apni.com/nsl/services/rest/name/apni/12345'
        'text/json'           | 'apni'    | 'name'        | null    | 'http://apni.com/nsl/services/json/name/apni/12345'
        'text/json'           | 'apni'    | 'treeElement' | 666     | 'http://apni.com/nsl/services/json/treeElement/666/12345'
        'application/rdf+xml' | 'apni'    | 'name'        | null    | null
        'text/xml'            | 'ausmoss' | 'name'        | null    | 'http://ausmoss.com/nsl/services/rest/name/ausmoss/12345'

    }
}
