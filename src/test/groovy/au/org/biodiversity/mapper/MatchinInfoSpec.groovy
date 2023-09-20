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

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
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
        String matchRegex = '^(https?://[^/]*)?/broker/(.*?)(/api/.*?)?(\\.json|\\.xml|\\.rdf|\\.html)?$'
        URI testUri = new URI(uri)
        MatchingInfo matchingInfo = new MatchingInfo(testUri, matchRegex)

        then:
        matchingInfo.path == path
        matchingInfo.api == api
        matchingInfo.extension == extension

        where:
        uri                                                                               | path                                                 | api                | extension
        '/broker/name/apni/2345'                                                          | 'name/apni/2345'                                     | ''                 | ''
        '/broker/name/apni/2345/api/apni-format'                                          | 'name/apni/2345'                                     | '/api/apni-format' | ''
        '/broker/name/apni/2345/api/apni-format.json'                                     | 'name/apni/2345'                                     | '/api/apni-format' | '.json'
        '/broker/name/apni/2345.json'                                                     | 'name/apni/2345'                                     | ''                 | '.json'
        '/broker/name/apni/2345.xml'                                                      | 'name/apni/2345'                                     | ''                 | '.xml'
        '/broker/name/apni/2345.rdf'                                                      | 'name/apni/2345'                                     | ''                 | '.rdf'
        '/broker/name/apni/2345.html'                                                     | 'name/apni/2345'                                     | ''                 | '.html'
        '/broker/apni.name/2345'                                                          | 'apni.name/2345'                                     | ''                 | ''
        '/broker/cgi-bin/apni?taxon_id=34551/api/apni-format.json'                        | 'cgi-bin/apni?taxon_id=34551'                        | '/api/apni-format' | '.json'
        '/broker/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin'              | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | ''                 | ''
        '/broker/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin.xml'          | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | ''                 | '.xml'
        '/broker/Euphorbia%20macgillivrayi%20var.%20pseudoserrulata%20Domin/api/blah.xml' | 'Euphorbia macgillivrayi var. pseudoserrulata Domin' | '/api/blah'        | '.xml'
        '/broker/name/apni/54433.rdf'                                                     | 'name/apni/54433'                                    | ''                 | '.rdf'

    }

    @Unroll
    void "matching info matches #url test"() {
        when:
        String matchRegex = '^(https?://[^/]*)?/?(.*?)(/api/.*?)?(\\.json|\\.xml|\\.rdf|\\.html)?$'
        URI testUri = new URI(url)
        MatchingInfo matchingInfo = new MatchingInfo(testUri, matchRegex)

        then:
        matchingInfo.path == path
        matchingInfo.api == api
        matchingInfo.extension == extension
        matchingInfo.host == host

        where:
        url                                                             | host                        | path             | api                | extension
        'http://localhost.com:8080/name/apni/2345'                      | 'http://localhost.com:8080' | 'name/apni/2345' | ''                 | ''
        'http://localhost.com:8080/name/apni/2345/api/apni-format'      | 'http://localhost.com:8080' | 'name/apni/2345' | '/api/apni-format' | ''
        'http://localhost.com:8080/name/apni/2345/api/apni-format.json' | 'http://localhost.com:8080' | 'name/apni/2345' | '/api/apni-format' | '.json'
    }
}
