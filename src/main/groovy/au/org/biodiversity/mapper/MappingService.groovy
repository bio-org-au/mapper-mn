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

/**
 * User: pmcneil
 * Date: 6/9/19
 *
 */
interface MappingService {

    /**
     * Gets a tuple of an Identifier and a Match from a uri path string
     * @param path
     * @return
     */
    Tuple2<Identifier, Match> getMatchIdentity(String uri)

    /**
     * Get a list of identities associated with a uri path string
     * @param path
     * @return A list of Identifiers, empty list if the match exists and there are no identities, and null if the
     * match doesn't exist.
     */
    List<Identifier> getMatchIdentities(String path)

    /**
     * returns the match associated with this id
     * @param id
     * @return a Match
     */
    Match getMatch(Long id)

    /**
     * returns the match associated with this uri string
     * @param uri
     * @return Match
     */
    Match findMatch(String uri)

    /**
     * Find the identiifer that matches the following.
     * @param nameSpace (required)
     * @param objectType (required)
     * @param idNumber (required)
     * @param versionNumber (optional may be null)
     * @return an Identifier or null if not found
     */
    Identifier findIdentifier(String nameSpace, String objectType, Long idNumber, Long versionNumber)

    /**
     * returns the host string that is marked preferred (there should only be one)
     * @return host string e.g. http://id.biodiversity.org.au
     */
    String getPreferredHost()

    /**
     * Returns the preferred URL for an identifier matching the inputs.
     * @param nameSpace (required)
     * @param objectType (required)
     * @param idNumber (required)
     * @return URL/link as string e.g. 'https://id.biodiversity.org.au/name/apni/12345' or null if not found
     */
    String getPreferredLink(String nameSpace, String objectType, Long idNumber)

    /**
     * returns a list of links associated with an identifier
     *
     * The returned Maps have the following structure:
     * [ link : String, resourceCount : number, preferred : boolean, deprecated : boolean,  deleted : boolean]
     *
     * resourceCount is the number of resources (identifiers) associated with this particular uri
     *
     * @param nameSpace
     * @param objectType
     * @param idNumber
     * @return A list of maps of link data
     */
    List<LinkResult> getlinks(String nameSpace, String objectType, long idNumber)

    /**
     * Add a new Identifier with the given link or a default link
     * @param nameSpace (required)
     * @param objectType (required)
     * @param idNumber (required)
     * @param versionNumber (Optional, may be null)
     * @param uri (Optional if not provided a default preferred URI will be created)
     * @param userName (required)
     * @return the new Identifier
     */
    Identifier addIdentifier(String nameSpace, String objectType, Long idNumber, Long versionNumber, String uri, String userName)

    /**
     * Remove the given identifier
     * @param identifier
     * @return success
     */
    Boolean removeIdentifier(Identifier identifier)

    /**
     * Remove any matches that have had their identifiers removed
     */
    void cleanupOrphanMatch()

    /**
     * Add a list of Identifiers with uri's.
     *
     * @param identifiers , a list of maps in the format [s: nameSpace, o: objectType, i: idNumber, v: versionNumber, u: uri]
     * @param username
     * @return success
     */
    Boolean bulkAddIdentifiers(Collection<Map> identifiers, String username)

    /**
     * Permanently remove a set of identifiers.
     * @param identifiers: a list of maps in the format [s: nameSpace, o: objectType, i: idNumber, v: versionNumber]
     * @return success
     */
    Boolean bulkRemoveIdentifiers(Collection<Map> identifiers)

    /**
     * Add a new host
     * @param hostName
     * @return a Host object
     */
    Host addHost(String hostName)

    /**
     * Set the host to be the preferred host
     * @param hostName
     * @return
     */
    Host setPreferredHost(String hostName)

    /**
     * Add the supplied match to the supplied identifier
     * @param identifier
     * @param match
     * @return success
     */
    Boolean addUriToIdentifier(Identifier identifier, Match match, Boolean setAsPreferred)

    /**
     * Adds a uri or returns the one that matches
     * @param uri
     * @param username
     * @return Match
     */
    Match addMatch(String uri, String username)

    /**
     * Returns a list of statistics about the mapper database
     * [ "identifiers": 17546853,
     *   "matches": 19594817,
     *   "hosts": 4,
     *   "orphanMatch": 612,
     *   "orphanIdentifier": 0]
     *
     * @return a map of stats
     */
    Map stats()

    /**
     * Move URI's from one Identifier to another Identifier
     * @param from
     * @param to
     * @return Boolean success
     */
    Boolean moveUris(Identifier from, Identifier to)

    /**
     * removes the Identifier from the the URI
     * @param match
     * @param identifier
     * @return Boolean success
     */
    Boolean removeIdentityFromUri(Match match, Identifier identifier)

    /**
     * Get the host for a given Match
     * @param match
     * @return Host
     */
    Host getHost(Match match)

    /**
     * Get an identifier by it's mapper id
     * @param id
     * @return Identifier
     */
    Identifier getIdentifier(Long id)

    /**
     * Set an Identifier as deleted. If URIs of this identifier are used in future they will get a HTTP GONE.
     * You must provide a reason to be supplied with the gone response.
     *
     * This doesn't actually remove the identifier, it just marks it as deleted.
     *
     * @param identifier
     * @param reason
     * @return Identifier
     */
    Identifier deleteIdentifier(Identifier identifier, String reason)
}