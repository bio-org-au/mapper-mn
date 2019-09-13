package au.org.biodiversity.mapper

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

    String toString() {
        "$uri, deprecated: $deprecated, Updated By: $updatedBy at $updatedAt"
    }
}
