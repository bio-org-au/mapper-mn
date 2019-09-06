package au.org.biodiversity.mapper

import groovy.sql.GroovyResultSet

import javax.inject.Inject
import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 4/9/19
 *
 */
class Identifier {

    MappingService mappingService

    Long id
    String nameSpace
    String objectType
    Long idNumber
    Long versionNumber
    Boolean deleted = false
    String reasonDeleted
    Timestamp updatedAt
    String updatedBy
    Long preferredUriID


    Identifier(MappingService mappingService, Map values, String prefix = 'i_') {
        this.mappingService = mappingService
        if (!values) {
            throw new NullPointerException("Map of values can't be null when constructing Identifier")
        }
        id = values."${prefix}id" as Long
        nameSpace = values."${prefix}name_space"
        objectType = values."${prefix}object_type"
        idNumber = values."${prefix}id_number" as Long
        versionNumber = values."${prefix}version_number" as Long
        deleted = values."${prefix}deleted"
        reasonDeleted = values."${prefix}reason_deleted"
        updatedAt = values."${prefix}updated_at" as Timestamp
        updatedBy = values."${prefix}updated_by"
        preferredUriID = values."${prefix}preferred_uri_id" as Long
    }

    Match getPreferredUri() {
        preferredUriID ? mappingService.getMatch(preferredUriID) : null
    }

    String toString() {
        if (deleted) {
            return "$id: $objectType, $nameSpace, $idNumber, deleted because: $reasonDeleted, Updated By: $updatedBy at $updatedAt"
        }
        return "$id: $objectType, $nameSpace, $idNumber, Updated By: $updatedBy at $updatedAt"
    }
}
