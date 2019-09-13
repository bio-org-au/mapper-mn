package au.org.biodiversity.mapper

import java.util.regex.Matcher

/**
 * User: pmcneil
 * Date: 5/9/19
 *
 */
class MatchingInfo {

    String host
    String path
    String api
    String extension

    MatchingInfo(URI uri, String matchRegex) {
        this(uri.toString(), matchRegex)
    }

    MatchingInfo(String uri, String matchRegex) {
        println "Regex is: $matchRegex"
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
