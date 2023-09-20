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

import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 6/9/19
 *
 */
@MicronautTest
class MatchSpec extends Specification {

    void "test create a Match"() {
        when: "I pass a null map"
        new Match((Map)null)

        then:
        thrown(NullPointerException)

        when: "I supply values"
        Timestamp t = new Timestamp(System.currentTimeMillis())
        Match match = new Match([
                m_id: 23,
                m_uri: '/type/object/12345',
                m_deprecated: false,
                m_updated_at: t,
                m_updated_by: 'fred'
        ])

        then: "a match is born"
        match
        match.id == 23
        match.uri == '/type/object/12345'
        !match.deprecated
        match.updatedAt == t
        match.updatedBy == 'fred'
        match.toString() == "/type/object/12345, deprecated: false, Updated By: fred at ${t.toString()}"

    }
}
