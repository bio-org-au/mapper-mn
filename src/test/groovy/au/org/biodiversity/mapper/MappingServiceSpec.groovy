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
class MappingServiceSpec extends Specification {

    @Inject
    MappingService mappingService

    void "test getting a match and identity from using #uri"() {
        when: "I look for #uri"
        Tuple2<Identifier, Match> tuple2 = mappingService.getMatchIdentity(uri)
        Identifier identifier = tuple2.first
        Match match = tuple2.second
        println identifier
        println match

        then: "I get"
        identifier
        identifier.idNumber == identIdNo
        identifier.deleted == deleted
        identifier.reasonDeleted == reason
        match
        match.deprecated == deprecated
        match.uri == uri

        where:
        uri                                  | deprecated | identIdNo | deleted | reason
        'Tieghemopanax macgillivrayi R.Vig.' | false      | 77821     | false   | null
        'cgi-bin/apni?taxon_id=230687'       | true       | 54433     | false   | null
        'name/apni/54433'                    | false      | 54433     | false   | null
        'name/apni/148297'                   | false      | 148297    | true    | 'Name has not been applied to Australian flora'
    }

    void "test match and Identity when there is no match"() {
        when:
        Tuple2<Identifier, Match> tuple2 = mappingService.getMatchIdentity('blahblahblah')

        then:
        tuple2 == null
    }

    void "test get a match by id"() {
        when:
        Match match = mappingService.getMatch(3)

        then:
        match
        match.id == 3
        match.uri == 'name/apni/54433'
        !match.deprecated

        when: "I try to get a non existent match"
        match = mappingService.getMatch(999999)

        then:
        match == null
    }

    void "test find a match by uri"() {
        when:
        Match match = mappingService.findMatch('name/apni/54433')

        then:
        match
        match.id == 3
        match.uri == 'name/apni/54433'
        !match.deprecated

        when: "I try to get a non existent match"
        match = mappingService.findMatch('i/dont/exist')

        then:
        match == null
    }

    void "test getting preferred host"() {
        when: "I get it"
        String host = mappingService.getPreferredHost()

        then:
        host
        host == 'http://localhost:8080'
    }

    void "test find identifier"(){
        when: "I try to find an identifier"
        Identifier i1 = mappingService.findIdentifier('apni','name',54433, null)

        then: "I get it"
        i1
        i1.nameSpace == 'apni'
        i1.objectType == 'name'
        i1.idNumber == 54433
        i1.versionNumber == null
        i1.preferredUriID == 4
        i1.updatedBy == 'pmcneil'

        when: "I try to find an identifier"
        Identifier i2 = mappingService.findIdentifier('apni','treeElement',2222, 23)

        then: "I get it"
        i2
        i2.nameSpace == 'apni'
        i2.objectType == 'treeElement'
        i2.idNumber == 2222
        i2.versionNumber == 23
        i2.preferredUriID == 8
        i2.updatedBy == 'pmcneil'

        when: "I look for one that doesn't exist"
        Identifier i3 = mappingService.findIdentifier('apni','name',54433, 23)

        then: "I get null"
        i3 == null
    }

    void "test addIdentifier"() {
        when: "I try to add an identifier that exits I get it back"
        Identifier i1 = mappingService.addIdentifier('apni', 'name', 54433, null, null, 'pmcneil')

        then:
        i1
        i1.id == 10

        when: "I try to add a new identifier with an existing uri"
        mappingService.addIdentifier('apni', 'name', 54434, null, 'name/apni/54433', 'pmcneil')

        then:
        MatchExistsException ex = thrown()
        ex.message == 'URI name/apni/54433 already exists'

        when: "I add new identifier without uri"
        Identifier i2 = mappingService.addIdentifier('apni', 'meh', 1, null, null, 'pmcneil')

        then: "It works and I get the default uri"
        i2
        i2.idNumber == 1
        i2.nameSpace == 'apni'
        i2.objectType =='meh'
        i2.preferredUri.uri == 'meh/apni/1'

        when: "I add new identifier with uri"
        Identifier i3 = mappingService.addIdentifier('apni', 'meh', 2, null, 'doodle/flip/twoddle', 'pmcneil')

        then: "It works and gives me back the uri I asked for"
        i3
        i3.idNumber == 2
        i3.nameSpace == 'apni'
        i3.objectType =='meh'
        i3.preferredUri.uri == 'doodle/flip/twoddle'

    }
}
