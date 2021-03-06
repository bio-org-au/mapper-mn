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
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.hateoas.JsonError
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import io.micronaut.security.authentication.Authentication

/**
 * User: pmcneil
 * Date: 9/9/19
 *
 */
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api")
class ApiController {
    @Inject
    MappingService mappingService

    @Property(name = 'mapper.url-regex')
    String matchRegex

    /**
     * Get the preferred host. This returns the host sans protocol e.g. localhost:8080
     *
     * http://.../api/preferred-host
     * <pre>
     * {
     *   "host": "http://id.biodiversity.org.au"
     * }</pre>
     * @return Map {"host": "String"}
     */
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/preferred-host")
    Map preferredHost() {
        String host = mappingService.preferredHost
        if (host) {
            return [host: host]
        }
        log.info("/preferred-host -> NULL. Check mapper host")
        return null
    }

    /**
     * get the preferred link for an identifier by object type, namespace and id number
     *
     * http://.../api/preferred-link/name/apni/70914
     *<pre>
     *  {
     *    "link": "http://biodiversity.org.au/boa/name/apni/70914"
     *  }</pre>
     *
     * @param nameSpace
     * @param objectType
     * @param idNumber
     * @return Map {"link": "String"}
     */
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/preferred-link/{objectType}/{nameSpace}/{idNumber}")
    Map preferredLink(@PathVariable String nameSpace, @PathVariable String objectType, @PathVariable Long idNumber) {
        String link = mappingService.getPreferredLink(nameSpace, objectType, idNumber)
        log.info("Getting /preferred-link for ${objectType}/${nameSpace}/${idNumber}")
        if (link) {
            return [link: link]
        }
        log.warn("/preferred-link -> Null")
        return null
    }

    /**
     * Get all the links for an Identifier by namespace, object type and id number
     *
     * e.g. http://.../api/links/name/apni/70914
     * <pre>
     * [
     *   {
     *     "link": "http://id.biodiversity.org.au/name/apni/70914",
     *     "resourceCount": 1,
     *     "preferred": true,
     *     "deprecated": false,
     *     "deleted": false
     *   },
     *   {
     *     "link": "http://biodiversity.org.au/boa/name/apni/70914",
     *     "resourceCount": 1,
     *     "preferred": false,
     *     "deprecated": false,
     *     "deleted": false
     *   }
     * ]</pre>
     * @param nameSpace
     * @param objectType
     * @param idNumber
     * @return JSON List of map
     */
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/links/{objectType}/{nameSpace}/{idNumber}")
    List<LinkResult> links(@PathVariable String nameSpace, @PathVariable String objectType, @PathVariable Long idNumber) {
        List<LinkResult> links = mappingService.getlinks(nameSpace, objectType, idNumber)
        log.info("/links/{objectType}/{nameSpace}/{idNumber} -> N0 of Links: ${links.size().toString()}")
        return links.empty ? null : links
    }

    /**
     * Get the current identity for a URI
     *
     * e.g. http://.../api/current-identity/?uri=http://id.biodiversity.org.au/name/apni/54433
     * <pre>
     * [
     *   {
     *     "id": 9,
     *     "nameSpace": "apni",
     *     "objectType": "name",
     *     "idNumber": 54433,
     *     "versionNumber": 0,
     *     "deleted": false,
     *     "updatedAt": 1450325464774
     *   }
     * ]</pre>
     *
     * @param uri a uri including the host to get the current identifier for.
     * @return a List of @see au.org.biodiversity.mapper.Identifier
     */
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/current-identity{?uri}")
    List<Identifier> currentIdentity(@QueryValue Optional<String> uri) {
        log.info "/current-identity{?uri} -> uri is ${uri.get()}"
        String u = URLDecoder.decode(uri.get(), 'UTF-8')
        MatchingInfo matchInfo = new MatchingInfo(u, matchRegex)
        List<Identifier> links = mappingService.getMatchIdentities(matchInfo.path)
        log.info "Links: ${links}"
        return links
    }

    /**
     * Get some stats on the mapper. This takes some time. e.g.
     * http://.../api/stats
     *
     * <pre>
     * {
     *   "identifiers": 17546853,
     *   "matches": 19594817,
     *   "hosts": 4,
     *   "orphanMatch": 612,
     *   "orphanIdentifier": 0
     * }</pre>
     * @return a map of stats
     */
    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Get("/stats")
    Map stats() {
        mappingService.stats()
    }

//**** Secured endpoints

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/add-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
    Map addIdentifierV1(@QueryValue Optional<String> nameSpace,
                        @QueryValue Optional<String> objectType,
                        @QueryValue Optional<Long> idNumber,
                        @QueryValue Optional<Long> versionNumber,
                        @QueryValue Optional<String> uri,  Authentication principal) {
        log.info "/add-identifier -> $nameSpace, $objectType, $idNumber, $versionNumber -> $uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace.get(),
                objectType.get(),
                idNumber.get(),
                versionNumber.orElse(null),
                uri.orElse(null),
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/add/{objectType}/{nameSpace}/{idNumber}")
    Map addNonVersionedIdentifier(@PathVariable String nameSpace,
                                  @PathVariable String objectType,
                                  @PathVariable Long idNumber,
                                  @Body Map body,
                                  Authentication principal) {
        String uri = body.uri
        log.info "Add $objectType/$nameSpace/$idNumber (uri: $uri)"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                null,
                uri,
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/add/{nameSpace}/{objectType}/{versionNumber}/{idNumber}")
    Map addVersionedIdentifier(@PathVariable String nameSpace,
                               @PathVariable String objectType,
                               @PathVariable Long idNumber,
                               @PathVariable Long versionNumber,
                               @Body Map body, Authentication principal
    ) {
        String uri = body.uri
        log.info "Add $objectType/$idNumber/$versionNumber -> namespace: $nameSpace, uri:$uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                versionNumber,
                uri,
                principal.getName())
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/add-host")
    Map addHost(@Body Map body) {
        String hostName = body.hostName
        Host host = mappingService.addHost(hostName)
        log.info("/add-host -> $hostName")
        return [host: host]
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/set-preferred-host")
    Map setPreferredHost(@Body Map body) {
        try {
            String hostName = body.hostName
            Host host = mappingService.setPreferredHost(hostName)
            log.info("Setting hostname to ${hostName}")
            return [host: host]
        } catch (NotFoundException nfe) {
            log.warn("Unable to set preferred hostname")
            return null
        }
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/bulk-add-identifiers")
    HttpResponse bulkAddIdentifiers(@Body Map body, Authentication principal) {
        Set<Map> identifiers = body.identifiers as Set<Map>
        log.info("/bulk-add-identifiers -> Adding ${identifiers.size().toString()} identifier[s]")
        if (mappingService.bulkAddIdentifiers(identifiers, principal.getName())) {
            log.info("bulk-add-identifiers - Successfully added all identifiers")
            return HttpResponse.<Map> ok(success: true, message: "${identifiers.size()} identities added.".toString())
        }
        log.error("bulk-add-identifiers - Returning Server Error")
        return HttpResponse.serverError()
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/bulk-remove-identifiers")
    HttpResponse bulkRemoveIdentifiers(@Body Map body) {
        Set<Map> identifiers = body.identifiers as Set<Map>
        log.info("/bulk-remove-identifiers -> Removing ${identifiers.size().toString()} identifier[s]")
        if (mappingService.bulkRemoveIdentifiers(identifiers)) {
            log.info("bulk-remove-identifiers - Successfully removed all identifiers")
            return HttpResponse.<Map> ok(success: true, message: "${identifiers.size()} identities removed.".toString())
        }
        log.error("bulk-add-identifiers - Returning Server Error")
        return HttpResponse.serverError()
    }

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Put("/add-uri-to-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}{?preferred}")
    HttpResponse addURI(@QueryValue Optional<String> nameSpace,
                        @QueryValue Optional<String> objectType,
                        @QueryValue Optional<Long> idNumber,
                        @QueryValue Optional<Long> versionNumber,
                        @QueryValue Optional<String> uri,
                        @QueryValue Optional<Boolean> preferred, Authentication principal) {
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

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
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

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
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

    @RolesAllowed('admin')
    @Produces(MediaType.APPLICATION_JSON)
    @Delete("/delete-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?reason}")
    HttpResponse deleteIdentifier(@QueryValue Optional<String> nameSpace,
                                  @QueryValue Optional<String> objectType,
                                  @QueryValue Optional<Long> idNumber,
                                  @QueryValue Optional<Long> versionNumber,
                                  @QueryValue Optional<String> reason) {
        Identifier identifier = mappingService.findIdentifier(nameSpace.get(), objectType.get(), idNumber.get(), versionNumber.orElse(null))
        log.info("/delete-identifier -> Deleting ${identifier.toString()}")
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
