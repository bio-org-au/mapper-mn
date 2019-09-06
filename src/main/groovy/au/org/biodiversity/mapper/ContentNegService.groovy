package au.org.biodiversity.mapper

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType

import javax.inject.Singleton

/**
 * User: pmcneil
 * Date: 5/9/19
 *
 */
@CompileStatic
@Singleton
class ContentNegService {

    @Property(name = 'mapper.format.html')
    Closure htmlResolver
    @Property(name = 'mapper.format.json')
    Closure jsonResolver
    @Property(name = 'mapper.format.xml')
    Closure xmlResolver
    @Property(name = 'mapper.format.rdf')
    Closure rdfResolver

    final static RDF_TYPE = new MediaType('application/rdf+xml', 'rdf')
    final static List<MediaType> acceptableTypes = [MediaType.TEXT_HTML_TYPE, MediaType.TEXT_JSON_TYPE, MediaType.TEXT_XML_TYPE, RDF_TYPE] as List<MediaType>

    /**
     * Check if any accepts header media types are available and return the first acceptable type.
     * If an extension is provided then return the content type for that extension in preference
     * to the accepts header.
     *
     * @param acceptTypes e.g. from request.headers.accept()
     * @param extension
     * @return a MediaType
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    MediaType chooseContentType(List<MediaType> acceptTypes, String extension) {
        MediaType pick = null
        if (extension) {
            String ext = extension[0] == '.' ? extension.substring(1) : extension
            pick = acceptableTypes.find { MediaType type ->
                type.extension == ext
            }
        }
        if (!pick && acceptTypes && acceptTypes.size()) {
            pick = acceptTypes.find { MediaType type ->
                acceptableTypes.contains(type)
            }
        }
        return pick ?: MediaType.TEXT_HTML_TYPE
    }

    String resolveServiceUrl(Identifier identifier, String mediaType) {
        String serviceUrl
        switch (mediaType) {
            case MediaType.TEXT_JSON:
                serviceUrl = jsonResolver(identifier)
                break
            case MediaType.TEXT_XML:
                serviceUrl = xmlResolver(identifier)
                break
            case 'application/rdf+xml':
                serviceUrl = rdfResolver(identifier)
                break
            default:
                serviceUrl = htmlResolver(identifier)
        }
        return serviceUrl
    }

}
