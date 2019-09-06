package au.org.biodiversity.mapper

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Produces

import javax.annotation.Nullable
import javax.inject.Inject

/**
 * User: pmcneil
 * Date: 2/9/19
 *
 */
@Controller('/')
class BrokerController {

    @Inject
    MappingService mappingService
    @Inject
    ContentNegService contentNegService

    @Produces(MediaType.TEXT_PLAIN)
    @Get("/{/path:.*}")
    HttpResponse index(@PathVariable @Nullable String path, HttpRequest<?> request) {

        println "Looking for ${request.uri}"
        MatchingInfo mInfo = new MatchingInfo(request.uri)

        Tuple2 matchIdentity = mappingService.getMatchIdentity(mInfo.path)
        if (matchIdentity) {
            Identifier identifier = matchIdentity.getFirst()
            Match match = matchIdentity.getSecond()
            if (identifier.deleted) {
                return gone(identifier.reasonDeleted)
            }
            if (match.deprecated) {
                return movedPermanently(identifier, mInfo, request)
            }
            return seeOther(identifier, mInfo, request)
        }
        println "Path you want: $mInfo.path Not Found!"
        return notFound("$mInfo.path not found")
    }


    private HttpResponse seeOther(Identifier identifier, MatchingInfo mInfo, HttpRequest<?> request) {

        MediaType contentType = contentNegService.chooseContentType(request.headers.accept(), mInfo.extension)
        String serviceUrl = contentNegService.resolveServiceUrl(identifier, contentType.name)

        println "Identifier: $identifier\n see: $serviceUrl"
        if (serviceUrl) {
            return HttpResponse
                    .status(HttpStatus.SEE_OTHER)
                    .header("Cache-Control", "no-cache, must-revalidate")
                    .header("Location", serviceUrl + mInfo.api + mInfo.extension)
        }
        return internalError('Service URL not found.')
    }

    private HttpResponse movedPermanently(Identifier identifier, MatchingInfo mInfo, HttpRequest<?> request) {
        Match preferredUrl = identifier.getPreferredUri()
        String host = mappingService.getPreferredHost()
        println "Identifier: $identifier\n moved permanently to: $host/$preferredUrl.uri"
        if (preferredUrl) {
            return HttpResponse
                    .status(HttpStatus.MOVED_PERMANENTLY)
                    .header("Cache-Control", "no-cache, must-revalidate")
                    .header("Location", "$host/$preferredUrl.uri${mInfo.api}")
        }
        return seeOther(identifier, mInfo, request)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private HttpResponse gone(String msg) {
        return HttpResponse
                .status(HttpStatus.GONE)
                .body(msg)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private HttpResponse notFound(String msg) {
        return HttpResponse
                .status(HttpStatus.NOT_FOUND)
                .body(msg)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private HttpResponse internalError(String msg) {
        return HttpResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(msg)
    }
}
