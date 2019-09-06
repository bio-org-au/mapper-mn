package au.org.biodiversity.mapper
import io.micronaut.test.annotation.MicronautTest
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
        new Match(null)

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
