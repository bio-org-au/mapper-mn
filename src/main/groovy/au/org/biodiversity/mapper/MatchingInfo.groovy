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

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

/**
 * User: pmcneil
 * Date: 5/9/19
 *
 */
@Slf4j
class MatchingInfo {

    String host
    String path
    String api
    String extension

    MatchingInfo(URI uri, String matchRegex) {
        this(uri.toString(), matchRegex)
    }

    MatchingInfo(String uri, String matchRegex) {
        log.debug "Regex is: $matchRegex"
        String urlString = URLDecoder.decode(uri, 'UTF-8')
        List<String> parts = findBits(urlString, matchRegex)
        host = parts[1]
        path = parts[2]
        api = parts[3] ?: ''
        extension = parts[4] ?: ''
    }

    private static List<String> findBits(String s, String matchRegex) {
        Matcher matcher = (s =~ matchRegex)
        matcher[0] as ArrayList<String>
    }
}
