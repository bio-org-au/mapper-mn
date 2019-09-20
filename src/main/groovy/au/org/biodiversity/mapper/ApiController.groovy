package au.org.biodiversity.mapper

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.hateoas.JsonError
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

import javax.annotation.security.PermitAll
import javax.inject.Inject
import java.security.Principal

/**
 * User: pmcneil
 * Date: 9/9/19
 *
 */
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api")
class ApiController {
    @Inject
    MappingService mappingService

    @Property(name = 'mapper.url-regex')
    String matchRegex

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/preferred-host")
    Map preferredHost() {
        String host = mappingService.preferredHost
        if (host) {
            return [host: host]
        }
        return null
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/preferred-link/{objectType}/{nameSpace}/{idNumber}")
    Map preferredLink(@PathVariable String nameSpace, @PathVariable String objectType, @PathVariable Long idNumber) {
        String link = mappingService.getPreferredLink(nameSpace, objectType, idNumber)
        if (link) {
            return [link: link]
        }
        return null
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/links/{objectType}/{nameSpace}/{idNumber}")
    List<Map> links(@PathVariable String nameSpace, @PathVariable String objectType, @PathVariable Long idNumber) {
        List<Map> links = mappingService.getlinks(nameSpace, objectType, idNumber)
        return links.empty ? null : links
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/current-identity{?uri}")
    List<Identifier> currentIdentity(@QueryValue Optional<String> uri) {
        println "uri is ${uri.get()}"
        MatchingInfo matchInfo = new MatchingInfo(uri.get(), matchRegex)
        List<Identifier> links = mappingService.getMatchIdentities(matchInfo.path)
        println links
        return links
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/stats")
    Map stats() {
        mappingService.stats()
    }

//**** Secured endpoints

    @Produces(MediaType.TEXT_JSON)
    @Put("/add-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
    Map addIdentifierV1(@QueryValue Optional<String> nameSpace,
                        @QueryValue Optional<String> objectType,
                        @QueryValue Optional<Long> idNumber,
                        @QueryValue Optional<Long> versionNumber,
                        @QueryValue Optional<String> uri, Principal principal) {
        println "Add identifier $nameSpace, $objectType, $idNumber, $versionNumber -> $uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace.get(),
                objectType.get(),
                idNumber.get(),
                versionNumber.orElse(null),
                uri.orElse(null),
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @Produces(MediaType.TEXT_JSON)
    @Put("/add/{objectType}/{nameSpace}/{idNumber}")
    Map addNonVersionedIdentifier(@PathVariable String nameSpace,
                                  @PathVariable String objectType,
                                  @PathVariable Long idNumber,
                                  @Body Map body, Principal principal) {
        String uri = body.uri
        println "Add $objectType/$nameSpace/$idNumber (uri: $uri)"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                null,
                uri,
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @Produces(MediaType.TEXT_JSON)
    @Put("/add/{nameSpace}/{objectType}/{versionNumber}/{idNumber}")
    Map addVersionedIdentifier(@PathVariable String nameSpace,
                               @PathVariable String objectType,
                               @PathVariable Long idNumber,
                               @PathVariable Long versionNumber,
                               @Body Map body, Principal principal
    ) {
        String uri = body.uri
        println "Add $objectType/$idNumber/$versionNumber -> namespace: $nameSpace, uri:$uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                versionNumber,
                uri,
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @Produces(MediaType.TEXT_JSON)
    @Put("/add-host")
    Map addHost(@Body Map body) {
        String hostName = body.hostName
        Host host = mappingService.addHost(hostName)
        return [host: host]
    }

    @Produces(MediaType.TEXT_JSON)
    @Put("/set-preferred-host")
    Map setPreferredHost(@Body Map body) {
        try {
            String hostName = body.hostName
            Host host = mappingService.setPreferredHost(hostName)
            return [host: host]
        } catch (NotFoundException nfe) {
            return null
        }
    }

    @Produces(MediaType.TEXT_JSON)
    @Post("/bulk-add-identifiers")
    HttpResponse bulkAddIdentifiers(@Body Map body, Principal principal) {
        Set<Map> identifiers = body.identifiers as Set<Map>
        if (mappingService.bulkAddIdentifiers(identifiers, principal.getName())) {
            return HttpResponse.<Map> ok(success: true, message: "${identifiers.size()} identities added.".toString())
        }
        return HttpResponse.serverError()
    }

    @Produces(MediaType.TEXT_JSON)
    @Post("/bulk-remove-identifiers")
    HttpResponse bulkRemoveIdentifiers(@Body Map body) {
        Set<Map> identifiers = body.identifiers as Set<Map>
        if (mappingService.bulkRemoveIdentifiers(identifiers)) {
            return HttpResponse.<Map> ok(success: true, message: "${identifiers.size()} identities removed.".toString())
        }
        return HttpResponse.serverError()
    }

    @Produces(MediaType.TEXT_JSON)
    @Put("/add-uri-to-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}{?preferred}")
    HttpResponse addURI(@QueryValue Optional<String> nameSpace,
                        @QueryValue Optional<String> objectType,
                        @QueryValue Optional<Long> idNumber,
                        @QueryValue Optional<Long> versionNumber,
                        @QueryValue Optional<String> uri,
                        @QueryValue Optional<Boolean> preferred, Principal principal) {
        Identifier identifier = mappingService.findIdentifier(nameSpace.get(), objectType.get(), idNumber.get(), versionNumber.orElse(null))
        if (identifier) {
            Match match = mappingService.addMatch(uri.get(), principal.getName())
            if (mappingService.addUriToIdentifier(identifier, match, preferred.orElse(false))) {
                return HttpResponse.<Map> ok([success: true, message: 'uri added to identity', match: match, identifier: identifier])
            }
            return HttpResponse.serverError(new JsonError("Could not add URI ${uri.get()} to Identifier"))
        }
        return HttpResponse.<JsonError> notFound(new JsonError('Identifier not found'))
    }

    @Produces(MediaType.TEXT_JSON)
    @Post("/move-identity")
    HttpResponse moveIdentity(@Body Map body) {
        Identifier from = mappingService.findIdentifier((String) body.fromNameSpace, (String) body.fromObjectType, (Long) body.fromIdNumber, (Long) body.fromVersionNumber)
        if (from) {
            Identifier to = mappingService.findIdentifier((String) body.toNameSpace, (String) body.toObjectType, (Long) body.toIdNumber, (Long) body.toVersionNumber)
            if (to) {
                if (mappingService.moveUris(from, to)) {
                    return HttpResponse.<Map> ok(success: true, message: "Identities moved.", from: from, to: to)
                }
                return HttpResponse.serverError(new JsonError("Couldn't move Identity"))
            }
            return HttpResponse.<JsonError> notFound(new JsonError("To identifier doesn't exist."))
        }
        return HttpResponse.<JsonError> notFound(new JsonError("From identifier doesn't exist."))
    }

    @Produces(MediaType.TEXT_JSON)
    @Delete("/remove-identifier-from-uri{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
    HttpResponse removeIdentityFromUri(@QueryValue Optional<String> nameSpace,
                                       @QueryValue Optional<String> objectType,
                                       @QueryValue Optional<Long> idNumber,
                                       @QueryValue Optional<Long> versionNumber,
                                       @QueryValue Optional<String> uri) {
        Identifier identifier = mappingService.findIdentifier(nameSpace.get(), objectType.get(), idNumber.get(), versionNumber.orElse(null))
        if (identifier) {
            Match match = mappingService.findMatch(uri.get())
            if (match) {
                if (mappingService.removeIdentityFromUri(match, identifier)) {
                    return HttpResponse.<Map> ok(success: true, message: 'Identifier removed from URI.', identifier: identifier)
                }
                return HttpResponse.serverError(new JsonError("Couldn't remove Identifier"))
            }
            return HttpResponse.<JsonError> notFound(new JsonError("URI ${uri.get()} doesn't exist."))
        }
        return HttpResponse.<JsonError> notFound(new JsonError("Identifier doesn't exist."))
    }

    @Produces(MediaType.TEXT_JSON)
    @Delete("/delete-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?reason}")
    HttpResponse deleteIdentifier(@QueryValue Optional<String> nameSpace,
                                  @QueryValue Optional<String> objectType,
                                  @QueryValue Optional<Long> idNumber,
                                  @QueryValue Optional<Long> versionNumber,
                                  @QueryValue Optional<String> reason) {
        Identifier identifier = mappingService.findIdentifier(nameSpace.get(), objectType.get(), idNumber.get(), versionNumber.orElse(null))
        if (identifier) {
            try {
                Identifier refreshedIdentifier = mappingService.deleteIdentifier(identifier, reason.orElse(null))
                if (refreshedIdentifier) {
                    return HttpResponse.<Map> ok(success: true, message: 'Deleted Identifier.', identifier: refreshedIdentifier)
                }
                return HttpResponse.serverError(new JsonError("Couldn't delete Identifier"))
            } catch (IllegalArgumentException iae) {
                return HttpResponse.badRequest(new JsonError(iae.message))
            }
        }
        return HttpResponse.<JsonError> notFound(new JsonError("Identifier doesn't exist."))
    }

}
