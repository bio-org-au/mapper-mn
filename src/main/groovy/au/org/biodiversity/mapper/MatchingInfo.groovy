package au.org.biodiversity.mapper

import java.util.regex.Matcher

/**
 * User: pmcneil
 * Date: 5/9/19
 *
 */
class MatchingInfo {
    String path
    String api
    String extension

    MatchingInfo(URI uri) {
        List<String> parts = findBits(uri.toString())
        path = parts[1]
        api = parts[2] ?: ''
        extension = parts[3] ?: ''
    }

    private static List<String> findBits(String s) {
        Matcher matcher = (s =~ '^/?(.*?)(/api/.*?)?(\\.json|\\.xml|\\.rdf|\\.html)?$')
        return matcher[0] as ArrayList<String>
    }
}
