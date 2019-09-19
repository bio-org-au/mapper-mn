package au.org.biodiversity.mapper

/**
 * User: pmcneil
 * Date: 13/9/19
 *
 */
class NotFoundException extends RuntimeException {
    NotFoundException(String message) {
        super(message)
    }
}
