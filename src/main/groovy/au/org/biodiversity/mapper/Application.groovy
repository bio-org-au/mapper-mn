package au.org.biodiversity.mapper

import io.micronaut.runtime.Micronaut
import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
        info = @Info(
                title = "Mapper",
                version = "2.0"
        )
)

@CompileStatic
class Application {
    static void main(String[] args) {
        Micronaut.run(Application)
    }

}