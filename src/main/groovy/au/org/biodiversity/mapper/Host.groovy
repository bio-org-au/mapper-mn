package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 16/9/19
 *
 */
class Host {
    Long id
    String hostName
    Boolean preferred

    Host(Map values, String prefix = 'h_') {
        println values.toString()
        assert values.size() == 3
        id = values."${prefix}id" as Long
        hostName = values."${prefix}host_name"
        preferred = values."${prefix}preferred" as Boolean
    }

    Host(List values) {
        println values.toString()
        assert values.size() == 3
        id = values[0] as Long
        hostName = values[1] as String
        preferred = values[2] as Boolean
    }

    @Override
    String toString() {
        "$id: $hostName, preferred: $preferred"
    }
}
