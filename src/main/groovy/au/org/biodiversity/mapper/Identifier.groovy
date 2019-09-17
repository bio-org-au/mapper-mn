package au.org.biodiversity.mapper

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import groovy.sql.GroovyResultSet

import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 4/9/19
 *
 */
class Identifier {

    @JsonIgnore
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
    @JsonIgnore
    Long preferredUriID
    
    /**
     * @param mappingService
     * @param values map of values
     * @param prefix default i_
     */
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

    /**
     * get values from a GroovyResultSet which implements getAt() like a map, but can't be cast to a Map without breaking
     * things. This is repeated code, yes :-(
     *
     * @param mappingService
     * @param values
     * @param prefix
     */
    Identifier(MappingService mappingService, GroovyResultSet values, String prefix = 'i_') {
        this.mappingService = mappingService
        if (!values) {
            throw new NullPointerException("Map of values can't be null when constructing Identifier")
        }
        id = values.getLong("${prefix}id")
        nameSpace = values.getString("${prefix}name_space")
        objectType = values.getString("${prefix}object_type")
        idNumber = values.getLong("${prefix}id_number")
        versionNumber = values.getLong("${prefix}version_number")
        deleted = values.getBoolean("${prefix}deleted")
        reasonDeleted = values.getString("${prefix}reason_deleted")
        updatedAt = values.getTimestamp("${prefix}updated_at")
        updatedBy = values.getString("${prefix}updated_by")
        preferredUriID = values.getLong("${prefix}preferred_uri_id")
    }

    @JsonIgnore
    Match getPreferredUri() {
        preferredUriID ? mappingService.getMatch(preferredUriID) : null
    }

    String toString() {
        if (deleted) {
            return "$id: $objectType, $nameSpace, $idNumber, deleted because: \"$reasonDeleted\", Updated By: $updatedBy at $updatedAt"
        }
        return "$id: $objectType, $nameSpace, $idNumber, Updated By: $updatedBy at $updatedAt"
    }
}
