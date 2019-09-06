package au.org.biodiversity.mapper

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileStatic

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

    /**
     * Returns a tuple of the match and the identifier.
     * NOTE: ignore the ID of the Identifier and Match objects here
     * @param path
     * @return
     */
    Tuple2<Identifier,Match> getMatchIdentity(String path) {
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
                println row.toString()
                Identifier identifier = new Identifier(this, row)
                Match match = new Match(row)
                return new Tuple2<Identifier, Match>(identifier, match)
            }
            return null
        }
    }

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

    String getPreferredHost() {
        withSql {Sql sql ->
            GroovyRowResult row = sql.firstRow('select * from mapper.host where preferred = true')
            return row?.host_name
        }
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

