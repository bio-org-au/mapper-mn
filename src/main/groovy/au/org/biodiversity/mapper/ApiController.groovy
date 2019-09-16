package au.org.biodiversity.mapper

import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.reactivex.Maybe

import javax.annotation.Nullable
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.validation.constraints.NotBlank

/**
 * User: pmcneil
 * Date: 9/9/19
 *
 */
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

//**** Secured endpoints TODO secure


    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Put("/add-identifier{?objectType}{?nameSpace}{?idNumber}{?versionNumber}{?uri}")
    Map addIdentifierV1(@QueryValue Optional<String> nameSpace,
                        @QueryValue Optional<String> objectType,
                        @QueryValue Optional<Long> idNumber,
                        @QueryValue Optional<Long> versionNumber,
                        @QueryValue Optional<String> uri) {
        println "Add identifier $nameSpace, $objectType, $idNumber, $versionNumber -> $uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace.get(),
                objectType.get(),
                idNumber.get(),
                versionNumber.orElse(null),
                uri.orElse(null),
                'fred') //todo add user
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Put("/add/{objectType}/{nameSpace}/{idNumber}")
    Map addNonVersionedIdentifier(@PathVariable String nameSpace,
                                  @PathVariable String objectType,
                                  @PathVariable Long idNumber,
                                  @Body Map body) {
        String uri = body.uri
        println "Add $objectType/$nameSpace/$idNumber (uri: $uri)"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                null,
                uri,
                'fred') //todo add user
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }

    @PermitAll
    @Produces(MediaType.TEXT_JSON)
    @Put("/add/{nameSpace}/{objectType}/{versionNumber}/{idNumber}")
    Map addVersionedIdentifier(@PathVariable String nameSpace,
                               @PathVariable String objectType,
                               @PathVariable Long idNumber,
                               @PathVariable Long versionNumber,
                               @Body Map body
    ) {
        String uri = body.uri
        println "Add $objectType/$idNumber/$versionNumber -> namespace: $nameSpace, uri:$uri"
        Identifier identifier = mappingService.addIdentifier(nameSpace,
                objectType,
                idNumber,
                versionNumber,
                uri,
                'fred')
        return [identifier: identifier, uri: identifier.preferredUri.uri]
    }


}
