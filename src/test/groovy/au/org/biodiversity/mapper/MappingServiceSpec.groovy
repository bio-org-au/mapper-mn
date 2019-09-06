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
        match
        match.deprecated == deprecated
        match.uri == uri

        where:
        uri                                  | deprecated | identIdNo
        'Tieghemopanax macgillivrayi R.Vig.' | false      | 77821
        'cgi-bin/apni?taxon_id=134625'       | true       | 155798
        'name/apni/54438'                    | false      | 54438
        'cgi-bin/apni?taxon_id=140141'       | true       | 145132
    }

    void "test match and Identity when there is no match"() {
        when:
        Tuple2<Identifier, Match> tuple2 = mappingService.getMatchIdentity('blahblahblah')

        then:
        tuple2 == null
    }

    void "test get a match by id"() {
        when:
        Match match = mappingService.getMatch(826075)

        then:
        match
        match.id == 826075
        match.uri == 'name/apni/54438'
        !match.deprecated

        when: "I try to get a non existent match"
        match = mappingService.getMatch(6)

        then:
        match == null
    }

    void "test getting preferred host"() {
        when: "I get it"
        String host = mappingService.getPreferredHost()

        then:
        host
        host == 'localhost:7070/nsl-mapper'
    }
}
