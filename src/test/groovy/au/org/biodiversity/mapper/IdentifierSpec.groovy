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
class IdentifierSpec extends Specification {

    void "test creating an Identifier"() {
        when: "I pass a null map"
        MappingService mappingService = Mock(MappingService)

        new Identifier(null, (Map)null)

        then: "throws and NPE"
        thrown(NullPointerException)

        when: "I supply values"
        Timestamp t = new Timestamp(System.currentTimeMillis())
        Identifier identifier = new Identifier(mappingService, [
                i_id              : 123654,
                i_name_space      : 'apni',
                i_object_type     : 'name',
                i_id_number       : 232323,
                i_version_number  : null,
                i_deleted         : false,
                i_reason_deleted  : null,
                i_updated_at      : t,
                i_updated_by      : 'fred',
                i_preferred_uri_id: 45
        ])

        then:
        identifier
        identifier.id == 123654
        identifier.nameSpace == 'apni'
        identifier.objectType == 'name'
        identifier.idNumber == 232323
        identifier.versionNumber == null
        !identifier.deleted
        identifier.reasonDeleted == null
        identifier.updatedAt == t
        identifier.updatedBy == 'fred'
        identifier.preferredUriID == 45
        identifier.toString() == "123654: name, apni, 232323, Updated By: fred at ${t.toString()}"

        when: "I get the preferredUri"
        Match prefUri = identifier.getPreferredUri()

        then: "It calls the mapping service"
        1 * mappingService.getMatch(_) >> { args ->
            new Match([m_id: args[0]])
        }
        prefUri.id == 45

        when: "the identifier is deleted."
        identifier.deleted = true
        identifier.reasonDeleted = 'I can test it.'

        then: "toString gives the reason."
        identifier.toString() == "123654: name, apni, 232323, deleted because: \"I can test it.\", Updated By: fred at ${t.toString()}"
    }



}
