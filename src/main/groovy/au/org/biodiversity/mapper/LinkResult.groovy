package au.org.biodiversity.mapper

import groovy.sql.GroovyResultSet

/**
 * User: pmcneil
 * Date: 24/9/19
 *
 */
class LinkResult {

    String link
    Integer resourceCount
    Boolean preferred
    Boolean deprecated
    Boolean deleted

    LinkResult(GroovyResultSet row, String defaultProtocol) {
        link         = "${defaultProtocol}://${row['link']}".toString()
         resourceCount= row['resourceCount'] as Integer
         preferred    = row['preferred'] as Boolean
         deprecated   = row['deprecated'] as Boolean
         deleted      = row['deleted'] as Boolean
    }

}
