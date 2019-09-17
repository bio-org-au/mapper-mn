package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 13/9/19
 *
 */
class NotFoundException extends Throwable{
    NotFoundException(String message) {
        super(message)
    }
}
