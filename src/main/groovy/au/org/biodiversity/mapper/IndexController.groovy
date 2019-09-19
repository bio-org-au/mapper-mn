package au.org.biodiversity.mapper


import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

import javax.annotation.security.PermitAll

/**
 * User: pmcneil
 * Date: 19/9/19
 *
 */
@Controller('/')
class IndexController {
    @PermitAll
    @Produces(MediaType.TEXT_HTML)
    @Get("/")
    HttpResponse index() {
        HttpResponse.ok('''<html><body>
<h1>Mapper</h1>
<p>
This is the mapper. It redirects you to resources. If you were using a resolvable URL then you should have been redirected,
and we have a configuration problem. (all resolvable URLs should be pointed at /broker/ context path). 
</p>
<p>The mapper has a JSON API, this is the only HTML page</p> 
<p>You can find mapper statistics at <a href="api/stats">stats</a></p>
</body></html>''')
    }
}
