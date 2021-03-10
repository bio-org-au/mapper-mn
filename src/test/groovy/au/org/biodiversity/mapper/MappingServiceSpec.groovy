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

import io.micronaut.test.extensions.spock.annotation.MicronautTest
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
//        'author/apni/51391959'               | false      | 51391959  | false   | null
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

    void "test find identifier"() {
        when: "I try to find an identifier"
        Identifier i1 = mappingService.findIdentifier('apni', 'name', 54433, null)

        then: "I get it"
        i1
        i1.nameSpace == 'apni'
        i1.objectType == 'name'
        i1.idNumber == 54433
        i1.versionNumber == null
        i1.preferredUriID == 3
        i1.updatedBy == 'pmcneil'

        when: "I try to find an identifier"
        Identifier i2 = mappingService.findIdentifier('apni', 'treeElement', 2222, 23)

        then: "I get it"
        i2
        i2.nameSpace == 'apni'
        i2.objectType == 'treeElement'
        i2.idNumber == 2222
        i2.versionNumber == 23
        i2.preferredUriID == 8
        i2.updatedBy == 'pmcneil'

        when: "I look for one that doesn't exist"
        Identifier i3 = mappingService.findIdentifier('apni', 'name', 54433, 23)

        then: "I get null"
        i3 == null
    }

    void "test add an existing Identifier"() {
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
    }

    @Unroll
    void "test add and remove an Identifier #preferredUri"() {
        when: "I add new identifier without uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace, objectType, idNumber, versionNumber, uri, 'pmcneil')

        then: "It works and I get the preferred URI #preferredUri"
        identifier
        identifier.idNumber == idNumber
        identifier.nameSpace == nameSpace
        identifier.objectType == objectType
        identifier.versionNumber == versionNumber
        identifier.preferredUri.uri == preferredUri

        when: "I remove the identifier"
        Boolean success = mappingService.removeIdentifier(identifier)
        Identifier i2 = mappingService.findIdentifier(nameSpace, objectType, idNumber, versionNumber)

        then: "success"
        success
        i2 == null

        where:
        nameSpace | objectType    | idNumber | versionNumber | uri                   | preferredUri
        'apni'    | 'meh'         | 1        | null          | null                  | 'meh/apni/1'
        'apni'    | 'meh'         | 2        | null          | 'doodle/flip/twoddle' | 'doodle/flip/twoddle'
        'apni'    | 'treeElement' | 1111     | 23            | null                  | 'treeElement/23/1111'
    }

    void "test get links"() {
        when: "I get the link"
        List<Map> links = mappingService.getlinks('apni', 'name', 54433)
        println links

        then: "I get 2 links"
        links
        links.size() == 2
        links[0].link == 'http://localhost:8080/name/apni/54433'
        links[0].preferred //preferred link first
        links[1].link == 'http://localhost:8080/cgi-bin/apni?taxon_id=230687'

        when: "I get an identifier with no links"
        List<Map> links2 = mappingService.getlinks('blah', 'name', 666)

        then: "Empty list"
        links2 != null
        links2.size() == 0

        when: "I ask for a non existent identifier"
        List<Map> links3 = mappingService.getlinks('blah', 'name', 999)

        then: "Empty list"
        links3 != null
        links3.size() == 0
    }

    void "test getPreferredLink"() {
        when: "I ask for a preferred identifier"
        String link1 = mappingService.getPreferredLink('apni', 'name', 54433)

        then: "I get it"
        link1
        link1 == 'http://localhost:8080/name/apni/54433'

        when: "I ask for a identifier that doesn't exist"
        String link2 = mappingService.getPreferredLink('apni', 'name', 99999)

        then:
        link2 == null
    }

    void "test get match Identities"() {
        when: "I get the identities for a uri"
        List<Identifier> identifiers1 = mappingService.getMatchIdentities('name/apni/54433')

        then: "I get one of them"
        identifiers1
        identifiers1.size() == 1
        identifiers1[0].objectType == 'name'
        identifiers1[0].nameSpace == 'apni'
        identifiers1[0].idNumber == 54433
        identifiers1[0].versionNumber == 0

        when: "I ask for one that doesn't exist"
        List<Identifier> identifiers2 = mappingService.getMatchIdentities('name/apni/99999')

        then: "I get null"
        identifiers2 == null

        when: "I ask for a match that has no identities, but exists"
        List<Identifier> identifiers3 = mappingService.getMatchIdentities('no-identifier/match')

        then: "I get an empty list"
        identifiers3.empty
    }

    void "test add/set preferred host"() {
        when: "I add a host that doesn't exist"
        Host h1 = mappingService.addHost('nerderg.com')
        println h1

        then: "I get a host back that isn't set as preferred"
        h1
        h1.hostName == 'nerderg.com'
        !h1.preferred

        when: "I try and add the same host name again"
        Host h2 = mappingService.addHost('nerderg.com')
        println h2

        then: "I get the same host back"
        h2
        h2.id == h1.id
        h1.hostName == h1.hostName

        when: "I set a host as preferred"
        Host h3 = mappingService.setPreferredHost('nerderg.com')
        println h3

        then: "I get a host back with preferred set"
        h3
        h3.preferred
        h3.id == h1.id

        when: "I try and set a non existent host as preferred"
        Host h4 = mappingService.setPreferredHost('derg.com')

        then: "I get a Not Found Exception"
        NotFoundException nfe = thrown()
        nfe.message == "Host derg.com not found"

        cleanup:
        mappingService.setPreferredHost('localhost:8080')

    }

    void "test add bulk identifiers"() {
        when: "I add 36k identifiers"
        Set<Map> bulkTreeIds = TestHelpers.getBulkTreeIds()
        Boolean success = mappingService.bulkAddIdentifiers(bulkTreeIds, 'tester')

        then: "It worked"
        success
        for (Map ident in bulkTreeIds) {
            Identifier identifier = mappingService.findIdentifier((String) ident.s, (String) ident.o, (Long) ident.i, (Long) ident.v)
            identifier != null
            identifier.preferredUri.uri == ident.u
        }

        when: "I remove them"
        Boolean s2 = mappingService.bulkRemoveIdentifiers(bulkTreeIds)

        then: "It should work"
        s2
        for (Map ident in bulkTreeIds) {
            Identifier identifier = mappingService.findIdentifier((String) ident.s, (String) ident.o, (Long) ident.i, (Long) ident.v)
            identifier == null
        }
        mappingService.stats().matches < 20
    }

    void "test addMatch"() {
        when: "I add a new uri"
        Match m1 = mappingService.addMatch('name/uri/123', 'test')
        Host host = mappingService.getHost(m1)

        then: "I get it"
        m1
        m1.uri == 'name/uri/123'
        host

        when: "I add it again"
        Match m2 = mappingService.addMatch('name/uri/123', 'test')

        then: "I get the same one back"
        m2
        m2.id == m1.id
    }

    void "test add uri to identifier"() {
        when: "I add a match to an identiifer that is already there"
        Identifier i1 = mappingService.findIdentifier('apni', 'name', 54433, null)
        Match m1 = mappingService.findMatch('name/apni/54433')
        Boolean s1 = mappingService.addUriToIdentifier(i1, m1, false)

        then: "It succeeds"
        s1

        when: "I add a new URI to an identifier"
        Match m2 = mappingService.addMatch('floop/floop/3', 'test')
        Boolean s2 = mappingService.addUriToIdentifier(i1, m2, false)
        Tuple2<Identifier, Match> t2 = mappingService.getMatchIdentity('floop/floop/3')

        then: "It succeeds"
        s2
        t2
        t2[0].id == i1.id

        when: "I add a new URI and make it preferred"
        Match m3 = mappingService.addMatch('floop/floop/boop', 'test')
        Boolean s3 = mappingService.addUriToIdentifier(i1, m3, true)
        Tuple2<Identifier, Match> t3 = mappingService.getMatchIdentity('floop/floop/boop')

        then: "It succeeds"
        s3
        t3
        t3[0].id == i1.id
        t3[0].preferredUriID == m3.id

        when: "I make the first URI preferred"
        Boolean s4 = mappingService.addUriToIdentifier(mappingService.getIdentifier(i1.id), m1, true)
        Identifier i4 = mappingService.getIdentifier(i1.id)

        then: "It succeeds"
        s4
        i4.preferredUriID == m1.id

    }

    void "test stats"() {
        when: "I call stats"
        Map stats = mappingService.stats()
        println stats

        then:
        stats
        stats.identifiers
        stats.matches
        stats.hosts
        stats.orphanMatch
        stats.orphanIdentifier
    }

    void "test remove identifier from uri"() {
        given:
        Identifier identifier = mappingService.addIdentifier('animals', 'cat',
                1, null, null, 'fred')
        Match match = mappingService.addMatch('pussie/1', 'fred')
        mappingService.addUriToIdentifier(identifier, match, false)

        expect:
        identifier
        match
        mappingService.getlinks('animals', 'cat', 1).size() == 2

        when: "I remove the preferred match"
        Boolean s1 = mappingService.removeIdentityFromUri(identifier.preferredUri, identifier)
        identifier = mappingService.getIdentifier(identifier.id)

        then:
        s1
        identifier
        identifier.preferredUriID == null
        mappingService.getlinks('animals', 'cat', 1).size() == 1
    }

    void "test delete identifier"() {
        given:
        Identifier identifier = mappingService.addIdentifier('animals', 'bird',
                1, null, null, 'fred')

        expect:
        identifier
        identifier.preferredUriID
        !identifier.deleted

        when: "I delete an identifier"
        identifier = mappingService.deleteIdentifier(identifier, 'I want to delete this')

        then: "it works and set the reason"
        identifier.deleted
        identifier.reasonDeleted == 'I want to delete this'

        when: "I delete a deleted identifier"
        identifier = mappingService.deleteIdentifier(identifier, 'I want to make this die')

        then: "It works and changes the reason"
        identifier.deleted
        identifier.reasonDeleted == 'I want to make this die'

        when: "I delete an identifier with blank reason"
        mappingService.deleteIdentifier(identifier, '')

        then: "chucks a wobbly"
        IllegalArgumentException ex1 = thrown()
        ex1.message == 'Reason cannot be null or blank.'

        when: "I delete an identifier with null reason"
        mappingService.deleteIdentifier(identifier, null)

        then: "chucks a wobbly"
        IllegalArgumentException ex2 = thrown()
        ex2.message == 'Reason cannot be null or blank.'

    }
}
