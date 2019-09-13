package au.org.biodiversity.mapper

import io.micronaut.runtime.Micronaut
import groovy.transform.CompileStatic
//import io.swagger.v3.oas.annotations.OpenAPIDefinition
//import io.swagger.v3.oas.annotations.info.Contact
//import io.swagger.v3.oas.annotations.info.Info
//import io.swagger.v3.oas.annotations.info.License
//
//@OpenAPIDefinition(
//        info = @Info(
//                title = "Mapper",
//                version = "2.0",
//                description = "Mapper API",
//                license = @License(name = "Apache 2.0", url = "http://biodiversity.org.au/license"),
//                contact = @Contact(url = "http://biodiversity.org.au", name = "support", email = "support@biodiversity.org.au")
//        )
//)
@CompileStatic
class Application {
    static void main(String[] args) {
        Micronaut.run(Application)
    }

}