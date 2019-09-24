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