package au.org.biodiversity.mapper

import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.runtime.ApplicationConfiguration

import javax.inject.Named
import javax.inject.Singleton

@Named("testClient")
@Singleton
class TestHttpClientConfiguration extends HttpClientConfiguration {
    TestHttpClientConfiguration(ApplicationConfiguration applicationConfiguration) {
        super(applicationConfiguration)
        setFollowRedirects(false)
    }

    @Override
    ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        ConnectionPoolConfiguration poolConfiguration = new DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration()
        return poolConfiguration
    }
}