package au.org.biodiversity.mapper

import io.micronaut.test.annotation.MicronautTest
import spock.lang.*
import javax.inject.Inject

/**
 * User: pmcneil
 * Date: 6/9/19
 *
 */
@MicronautTest
class MatchinInfoSpec extends Specification {

    @Unroll
    void "matching info matches #uri test"() {
        when:
        URI testUri = new URI(uri)
        MatchingInfo matchingInfo = new MatchingInfo(testUri)

        then:
        matchingInfo.path == path
        matchingInfo.api == api
        matchingInfo.extension == extension

        where:
        uri                                                                        | path                                                 | api                | extension
        '/name/apni/2345'                                                          | 'name/apni/2345'                                     | ''                 | ''
        '/name/apni/2345/api/apni-format'                                          | 'name/apni/2345'                                     | '/api/apni-format' | ''
        '/name/apni/2345/api/apni-format.json'                                     | 'name/apni/2345'                                     | '/api/apni-format' | '.json'
        '/name/apni/2345.json'                                                     | 'name/apni/2345'                                     | ''                 | '.json'
        '/name/apni/2345.xml'                                                      | 'name/apni/2345'                                     | ''                 | '.xml'
        '/name/apni/2345.rdf'                                                      | 'name/apni/2345'                                     | ''                 | '.rdf'
        '/name/apni/2345.html'                                                     | 'name/apni/2345'                                     | ''                 | '.html'
        '/apni.name/2345'                                                          | 'apni.name/2345'                                     | ''                 | ''
        '/cgi-bin/apni?taxon_id=34551/api/apni-format.json'                        | 'cgi-bin/apni?taxon_id=34551'                        | '/api/apni-format' | '.json'
        '/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin'              | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | ''                 | ''
        '/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin.xml'          | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | ''                 | '.xml'
        '/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin/api/blah.xml' | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | '/api/blah'        | '.xml'

    }
}
