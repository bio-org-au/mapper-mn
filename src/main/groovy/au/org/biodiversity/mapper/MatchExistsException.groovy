package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 13/9/19
 *
 */
class MatchExistsException extends Throwable{
    MatchExistsException(String message) {
        super(message)
    }
}
