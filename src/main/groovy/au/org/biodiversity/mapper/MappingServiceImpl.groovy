package au.org.biodiversity.mapper

import groovy.sql.GroovyResultSet
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.scheduling.annotation.Async

import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * User: pmcneil
 * Date: 4/9/19
 *
 */
@Slf4j
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
     * @return String link or null
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
        if (versionNumber) {
            query += 'and version_number = :versionNumber'
        } else {
            query += 'and version_number is null'
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

    /**
     * This permanently removes an identifier and it's exclusive Matches. It can't be undone and if someone tries to
     * resolve a match on this identity they will get a 404 - this is meant for draft identities only
     * @param identifier
     */
    @Synchronized
    Boolean removeIdentifier(Identifier identifier) {
        //remove the identifier from identifier_identities
        //delete the identifier
        Boolean success = false
        withSql { Sql sql ->
            sql.withTransaction {
                sql.execute('''
                    delete from mapper.identifier_identities ii where identifier_id = :identifierID;
                    delete from mapper.identifier i where id = :identifierID;''', [identifierID: identifier.id])
                success = true
            }

        }
        if (success) {
            cleanupOrphanMatch()
        }
        return success
    }

    @Async
    void cleanupOrphanMatch() {
        withSql { Sql sql ->
            sql.withTransaction {
                sql.execute('''
                    create temp table orphanMatch on commit drop as select id from mapper.match m
                    where not exists(select 1 from mapper.identifier_identities where match_id = m.id)
                        and not exists(select 1 from mapper.identifier where preferred_uri_id = m.id);
                    delete from mapper.match_host where match_hosts_id in (select id from orphanMatch);
                    delete from mapper.match where id in (select id from orphanMatch);''')
            }
        }
    }

/**
 * Add a list of Identifiers with uri's.
 *
 * @param identifiers , a list of maps in the format [s: nameSpace, o: objectType, i: idNumber, v: versionNumber, u: uri]
 * @param username
 */
    Boolean bulkAddIdentifiers(Collection<Map> identifiers, String username) {
        log.info "Adding ${identifiers.size()} identifiers"
        withSql { Sql sql ->
            Boolean success = false
            sql.withTransaction {
                log.info "inserting matches"
                sql.execute(insertBulkMatch(identifiers, username))

                log.info "inserting identifiers"
                sql.execute(insertBulkIdentifier(identifiers, username))

                log.info "linking identifiers to matches"
                sql.execute('''insert into mapper.identifier_identities (match_id, identifier_id) (select preferred_uri_id, i.id from mapper.identifier i
                    where i.preferred_uri_id is not null and not exists (select 1 from mapper.identifier_identities ii where ii.identifier_id = i.id))''')

                log.info "Inserting match hosts"
                sql.executeInsert('''INSERT INTO mapper.match_host (match_hosts_id, host_id)
                    SELECT m.id, ph.id
                    FROM mapper.match m,
                    (SELECT h.id FROM mapper.host h WHERE h.preferred) ph     
                    WHERE NOT exists(SELECT 1 FROM mapper.match_host mh WHERE mh.match_hosts_id = m.id)''')
                success = true
            }
            return success
        }
    }

    private static String insertBulkMatch(Collection<Map> identifiers, String username) {
        List<String> insert =  []
        for (Map ident in identifiers) {
            insert.add("('${ident.u}', now(), '$username')".toString())
        }
        log.info "finished making insert match"
        return 'INSERT INTO mapper.match (uri, updated_at, updated_by) VALUES ' + insert.join(',')
    }

    private static String insertBulkIdentifier(Collection<Map> identifiers, String username) {
        List<String> insert =  []
        for (Map ident in identifiers) {
            insert.add("('${ident.s}', '${ident.o}', ${ident.i}, ${ident.v}, (SELECT id FROM mapper.match WHERE uri = '${ident.u}'), '${username}', now())".toString())
        }
        log.info "finished making insert Identifier"
        return 'INSERT INTO mapper.identifier (name_space, object_type, id_number, version_number, preferred_uri_id, updated_by, updated_at) VALUES ' + insert.join(',')
    }


    /**
     * permanently remove a set of identifiers, for example when you delete a draft tree.
     * @param identifiers : a list of maps in the format [s: nameSpace, o: objectType, i: idNumber, v: versionNumber]
     */
    @Synchronized
    void bulkRemoveIdentifiers(List<Map> identifiers) {
        log.debug "Removing ${identifiers.size()} identifiers"
        withSql { Sql sql ->
            sql.withTransaction {
                int count = 0

                sql.execute('''
                    create temp table mapper.bulk_remove
                    (
                        identifier_id INT8 NOT NULL,
                        match_id      INT8 NOT NULL,
                        PRIMARY KEY (identifier_id)
                    ) on commit drop;''')

                for (Map ident in identifiers) {
                    sql.execute('''
                    INSERT INTO mapper.bulk_remove 
                      SELECT ii.identifier_id, ii.match_id FROM mapper.identifier i JOIN mapper.identifier_identities ii ON i.id = ii.identifier_id
                            WHERE i.id_number = :i 
                              AND i.object_type = :o
                              AND i.version_number = :v
                              AND i.name_space = :s''', ident)
                    if (++count % 1000 == 0) {
                        log.debug "Found $count identifiers to remove."
                    }
                }
                log.debug "Removing identifiers"
                sql.execute('''
                DELETE FROM mapper.identifier_identities ii WHERE identifier_id IN (SELECT DISTINCT (identifier_id) FROM mapper.bulk_remove);
                DELETE FROM mapper.identifier i WHERE i.id IN (SELECT DISTINCT (identifier_id) FROM mapper.bulk_remove);''')
            }
        }
        cleanupOrphanMatch()
    }


    @Override
    Host addHost(String hostName) {
        withSql { Sql sql ->
            Host newHost = findHost(hostName, sql)
            if (!newHost) {
                sql.withTransaction {
                    List<List<Object>> h = sql.executeInsert('''insert into mapper.host (host_name, preferred) values  (:hostName, false)''', [hostName: hostName])
                    newHost = new Host(h[0])
                }
            }
            return newHost
        }
    }

    /**
     * sets the host with the hostname as the preferred host and all others as not. There can only be one.
     * throws a NotFoundException if the host doesn't exist.
     * @param hostName
     * @return Host
     */
    @Override
    Host setPreferredHost(String hostName) {
        withSql { Sql sql ->
            Host host = findHost(hostName, sql)
            if (!host) {
                throw new NotFoundException("Host $hostName not found")
            }
            if (host.preferred) {
                return host
            }
            sql.withTransaction {
                sql.execute('''
                    update mapper.host set preferred = false where preferred;
                    update mapper.host set preferred = true where host_name = :hostName''', [hostName: hostName])
            }
            return findHost(hostName, sql) //get it from the DB
        }
    }

// *** private ***

    static Host findHost(String hostName, Sql sql) {
        GroovyRowResult row = sql.firstRow('select * from mapper.host where host_name = :hostName', [hostName: hostName])
        if (row) {
            return new Host(row, '')
        }
        return null
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

