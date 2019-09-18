package au.org.biodiversity.mapper

import groovy.sql.GroovyResultSet

import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 4/9/19
 *
 */
class Match {

    Long id
    String uri //the unique identifier section of the match less the resolver host e.g. name/apni/123456
    Boolean deprecated = false //if this URI is depecated and should no longer be referred to publicly
    Timestamp updatedAt
    String updatedBy

    Match(Map values, String prefix = 'm_') {
        if (!values) {
            throw new NullPointerException("Map of values can't be null when constructing Match")
        }
        id = values."${prefix}id"
        uri = values."${prefix}uri"
        deprecated = values."${prefix}deprecated"
        updatedAt = values."${prefix}updated_at" as Timestamp
        updatedBy = values."${prefix}updated_by"
    }

    Match(GroovyResultSet values, String prefix = 'm_') {
        if (!values) {
            throw new NullPointerException("Row of values can't be null when constructing Match")
        }
        id = values.getLong("${prefix}id")
        uri = values.getString("${prefix}uri")
        deprecated = values.getBoolean("${prefix}deprecated")
        updatedAt = values.getTimestamp("${prefix}updated_at")
        updatedBy = values.getString("${prefix}updated_by")
    }

    Match(List values) {
        println values
        if (!values) {
            throw new NullPointerException("List of values can't be null when constructing Match")
        }
        id = values[0] as Long
        uri = values[1] as String
        deprecated = values[2] as Boolean
        updatedAt = values[3] as Timestamp
        updatedBy = values[4] as String
    }

    String toString() {
        "$uri, deprecated: $deprecated, Updated By: $updatedBy at $updatedAt"
    }
}
