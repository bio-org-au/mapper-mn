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
    Tuple2<Identifier, Match> getMatchIdentity(String path)

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
     * @return an Identifier
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
    List<Map> getlinks(String nameSpace, String objectType, long idNumber)

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

}