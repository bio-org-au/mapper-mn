package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 6/9/19
 *
 */
interface MappingService {
    Tuple2<Identifier, Match> getMatchIdentity(String path)

    Match getMatch(Long id)

    String getPreferredHost()
}