package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 17/9/19
 *
 */
class TestHelpers {

    static Set<Map> getBulkTreeIds () {
        GroovyShell shell = new GroovyShell()
        File ids = new File('./src/test/groovy/au/org/biodiversity/mapper/BulkIdentifiers.txt')
        //split the read to avoid method too large
        List<String> lines = ids.readLines()
        Set<Map> bulkTreeIds = []
        int i = 0
        while( i < lines.size()) {
            int mod = 0
            List<String> cat = []
            while (mod < 500 && i + mod < lines.size()) {
                cat.add(lines[i + mod++])
            }
            Set<Map> set = shell.evaluate('[' + cat.join(',') + ']')
            bulkTreeIds.addAll(set)
            i += mod
        }
        return bulkTreeIds
    }
}
