package au.org.biodiversity.mapper

import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.runtime.ApplicationConfiguration

import javax.inject.Named
import javax.inject.Singleton
import java.time.Duration

@Named("testClient")
@Singleton
class TestHttpClientConfiguration extends HttpClientConfiguration {
    TestHttpClientConfiguration(ApplicationConfiguration applicationConfiguration) {
        super(applicationConfiguration)
        setFollowRedirects(false)
        setReadTimeout(Duration.ofSeconds(40))
    }

    @Override
    ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        ConnectionPoolConfiguration poolConfiguration = new DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration()
        return poolConfiguration
    }
}