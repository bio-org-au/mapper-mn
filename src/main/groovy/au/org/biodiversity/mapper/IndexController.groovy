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
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.sql.DataSource

/**
 * User: pmcneil
 * Date: 19/9/19
 *
 */
@Controller('/')
class IndexController {

    @Inject
    DataSource dataSource

    @Property(name = "micronaut.config.files")
    String configFiles

    @Property(name = "mapper.db.url")
    String dbUrl

    @PermitAll
    @Produces(MediaType.TEXT_HTML)
    @Get("/")
    HttpResponse index() {
        HttpResponse.ok("""Mapper

This is the mapper. It redirects you to resources. If you were using a resolvable URL then you should have been redirected,
and we have a configuration problem. (all resolvable URLs should be pointed at /broker/ context path). 

The mapper has a JSON API, this is the only HTML page.
You can find mapper statistics at "api/stats"
Connected to DB ${dbUrl}
Config files ${configFiles}
""")
    }
}
