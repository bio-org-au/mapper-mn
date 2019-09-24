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
