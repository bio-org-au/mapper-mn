package au.org.biodiversity.mapper

import groovy.sql.GroovyResultSet
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Property

import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * User: pmcneil
 * Date: 4/9/19
 *
 */
@CompileStatic
@Singleton
class MappingServiceImpl implements MappingService {

    @Inject
    DataSource dataSource

    @Property(name = 'mapper.default-protocol')
    String defaultProtocol
    @Property(name = 'mapper.resolver-url')
    String resolverUrl

    /**
     * Returns a tuple of the match and the identifier.
     * NOTE: ignore the ID of the Identifier and Match objects here
     * @param path
     * @return
     */
    Tuple2<Identifier, Match> getMatchIdentity(String path) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow('''
            select m.id               as m_id,
                   m.uri              as m_uri,
                   m.deprecated       as m_deprecated,
                   m.updated_at       as m_updated_at,
                   m.updated_by       as m_updated_by,
                   i.id               as i_id,
                   i.id_number        as i_id_number,
                   i.name_space       as i_name_space,
                   i.object_type      as i_object_type,
                   i.deleted          as i_deleted,
                   i.reason_deleted   as i_reason_deleted,
                   i.updated_at       as i_updated_at,
                   i.updated_by       as i_updated_by,
                   i.preferred_uri_id as i_preferred_uri_id,
                   i.version_number   as i_version_number
            from mapper.match m
                     join mapper.identifier_identities ii on m.id = ii.match_id
                     join mapper.identifier i on ii.identifier_id = i.id
            where uri = :path''', [path: path])
            if (row) {
                Identifier identifier = new Identifier(this, row)
                Match match = new Match(row)
                return new Tuple2<Identifier, Match>(identifier, match)
            }
            return null
        }
    }

    List<Identifier> getMatchIdentities(String path) {
        List<Identifier> identifiers = []
        Boolean matchFound = false
        withSql { Sql sql ->
            sql.eachRow('''
            select m.id as m_id,
                   i.id               as i_id,
                   i.id_number        as i_id_number,
                   i.name_space       as i_name_space,
                   i.object_type      as i_object_type,
                   i.deleted          as i_deleted,
                   i.reason_deleted   as i_reason_deleted,
                   i.updated_at       as i_updated_at,
                   i.updated_by       as i_updated_by,
                   i.preferred_uri_id as i_preferred_uri_id,
                   i.version_number   as i_version_number
            from mapper.match m
                     left outer join mapper.identifier_identities ii on m.id = ii.match_id
                     left outer join mapper.identifier i on ii.identifier_id = i.id
            where uri = :path''', [path: path]) { GroovyResultSet row ->
                matchFound = (matchFound || row.getLong("m_id") != 0)
                if (row["i_id"]) {
                    identifiers.add(new Identifier(this, row))
                }
            }
        }
        return (matchFound ? identifiers : null)
    }


    /**
     * Gets the preferred host String
     * @return the preferred host string e.g. http://id.biodiversity.org.au
     */
    String getPreferredHost() {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow('select * from mapper.host where preferred = true')
            if (row) {
                return "${defaultProtocol}://${row.host_name}"
            }
            return resolverUrl
        }
    }

    /**
     * Get the preferred link for the given identifier determined by name space object type and id
     *
     * Note this shouldn't be used for versioned resources
     *
     * @param nameSpace
     * @param objectType
     * @param idNumber
     * @return String link
     */
    String getPreferredLink(String nameSpace, String objectType, Long idNumber) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow('''
            select h.host_name || '/' || m.uri as uri
            from mapper.identifier i 
                join mapper.match m on i.preferred_uri_id = m.id
                join mapper.match_host mh on m.id = mh.match_hosts_id
                join mapper.host h on mh.host_id = h.id
            where name_space = :nameSpace and object_type = :objectType and id_number = :idNumber''',
                    [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber])
            return row ? "${defaultProtocol}://${row.uri}" : null
        }
    }

    /**
     * get the list of links associated with an identifier.
     * @param nameSpace
     * @param objectType
     * @param idNumber
     * @return a List of Maps containing [link, resourceCount, preferred, deprecated, deleted]
     */
    List<Map> getlinks(String nameSpace, String objectType, long idNumber) {
        List<Map> links = []
        withSql { Sql sql ->
            sql.eachRow('''
            select host.host_name || '/' || m.uri as link,
                   (select count(*) from mapper.identifier_identities where match_id = m.id) as resourceCount,
                   (m.id = i.preferred_uri_id and host.preferred) as preferred,
                   m.deprecated,
                   i.deleted
            from mapper.identifier i 
                join mapper.identifier_identities ii on i.id = ii.identifier_id
                join mapper.match m on ii.match_id = m.id
                join mapper.match_host mh on m.id = mh.match_hosts_id
                join mapper.host on mh.host_id = host.id
            where name_space = :nameSpace and object_type = :objectType and id_number = :idNumber
            order by preferred desc, resourceCount''',
                    [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]) { GroovyResultSet row ->
                links.add([link         : "${defaultProtocol}://${row['link']}".toString(),
                           resourceCount: row['resourceCount'],
                           preferred    : row['preferred'],
                           deprecated   : row['deprecated'],
                           deleted      : row['deleted']
                ])
            }
        }
        return links
    }

    Identifier findIdentifier(String nameSpace, String objectType, Long idNumber, Long versionNumber) {
        String query = '''select 
                    i.id               as i_id,
                    i.id_number        as i_id_number,
                    i.name_space       as i_name_space,
                    i.object_type      as i_object_type,
                    i.deleted          as i_deleted,
                    i.reason_deleted   as i_reason_deleted,
                    i.updated_at       as i_updated_at,
                    i.updated_by       as i_updated_by,
                    i.preferred_uri_id as i_preferred_uri_id,
                    i.version_number   as i_version_number
            from mapper.identifier i 
            where name_space = :nameSpace 
              and object_type = :objectType 
              and id_number = :idNumber
              '''
        if(versionNumber) {
            query +=  'and version_number = :versionNumber'
        } else {
            query +=  'and version_number is null'
        }
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow(query,
                    [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber, versionNumber: versionNumber])
            return row ? new Identifier(this, row) : null
        }
    }

    /**
     * Gets a Match by ID
     * @param id
     * @return a Match object
     */
    Match getMatch(Long id) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow('''
            select m.id               as m_id,
                   m.uri              as m_uri,
                   m.deprecated       as m_deprecated,
                   m.updated_at       as m_updated_at,
                   m.updated_by       as m_updated_by
            from mapper.match m where id = :id''', [id: id])
            return row ? new Match(row) : null
        }
    }

    /**
     * Finds a Match by uri
     * @param uri String
     * @return a Match object
     */
    Match findMatch(String uri) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow('''
            select m.id               as m_id,
                   m.uri              as m_uri,
                   m.deprecated       as m_deprecated,
                   m.updated_at       as m_updated_at,
                   m.updated_by       as m_updated_by
            from mapper.match m where uri = :uri''', [uri: uri])
            return row ? new Match(row) : null
        }
    }

    Identifier addIdentifier(String nameSpace, String objectType, Long idNumber, Long versionNumber, String uri, String userName) {
        Identifier exists = findIdentifier(nameSpace, objectType, idNumber, versionNumber)
        if (exists) {
            return exists
        }

        if (uri) {
            Match match = findMatch(uri)
            if (match) {
                // don't need to check identity matches since previous search should have found it
                throw new MatchExistsException("URI $uri already exists")
            }
        } else {
            uri = defaultUri(nameSpace, objectType, idNumber, versionNumber)
        }
        // do the inserts
        withSql { Sql sql ->
            sql.withTransaction {
                List<List<Object>> m = sql.executeInsert('''Insert into mapper.match
                (uri, deprecated, updated_at, updated_by) VALUES (:uri, false, now(), :userName)''',
                        [uri: uri, userName: userName]
                )
                println m
                Long matchId = m[0][0] as Long
                sql.executeInsert('''insert into mapper.match_host (match_hosts_id, host_id) 
                    VALUES (:matchId, (select id from mapper.host where preferred limit 1))''', [matchId: matchId])
                List<List<Object>> i = sql.executeInsert('''Insert into mapper.identifier
                (id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number)
                values (:idNumber, :nameSpace, :objectType, false, null, now(), :userName, :matchId, :versionNumber)
                 ''', [idNumber: idNumber, nameSpace: nameSpace, objectType: objectType,
                       userName: userName, matchId: matchId, versionNumber: versionNumber])
                sql.executeInsert('''insert into mapper.identifier_identities (match_id, identifier_id) 
                    VALUES (:matchId, :identifierId)''', [matchId: matchId, identifierId: i[0][0] as Long])
            }
        }
        return findIdentifier(nameSpace, objectType, idNumber, versionNumber)
    }

    private static String defaultUri(String nameSpace, String objectType, Long idNumber, Long versionNumber) {
        if (versionNumber) {
            return "$objectType/$versionNumber/$idNumber"
        }
        return "$objectType/$nameSpace/$idNumber"
    }

    @SuppressWarnings("GrUnnecessaryPublicModifier")
    public <T> T withSql(Closure<T> work) {
        Sql sql = Sql.newInstance(dataSource)
        try {
            return work(sql)
        } finally {
            sql.close()
        }
    }

}

