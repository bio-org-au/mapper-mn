/*
    Copyright (c) 2019 Australian National Botanic Gardens and Authors

    This file is part of National Species List project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
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
