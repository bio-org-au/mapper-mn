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

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Produces

import javax.annotation.Nullable
import javax.annotation.security.PermitAll
import javax.inject.Inject

/**
 * User: pmcneil
 * Date: 2/9/19
 *
 */
@Controller('/broker')
class BrokerController {

    @Inject
    MappingService mappingService
    @Inject
    ContentNegService contentNegService
    @Property(name = 'mapper.broker-regex')
    String matchRegex

    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    @Get("/{/path:.*}")
    HttpResponse index(@PathVariable @Nullable String path, HttpRequest<?> request) {

        println "Request to ${request.uri}"
        MatchingInfo mInfo = new MatchingInfo(request.uri, matchRegex)
        println "Looking for ${mInfo.path}"

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
        if (preferredUrl) {
            println "Identifier: $identifier\n moved permanently to: $host/$preferredUrl.uri"
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
