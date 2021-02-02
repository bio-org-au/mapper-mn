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

import edu.umd.cs.findbugs.annotations.Nullable
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.UserDetails
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import groovy.util.logging.Slf4j
import io.reactivex.BackpressureStrategy

import javax.inject.Singleton

/**
 * User: pmcneil
 * Date: 12/9/19
 *
 */
@Slf4j
@Singleton
class AuthenticationProviderUserPassword implements AuthenticationProvider {

    @Property(name = 'mapper.auth')
    Map authMap

    @Override
    Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        String username = authenticationRequest.getIdentity()
        log.info("Username -> ${username} requesting token")
        Map authData = authMap[username] as Map
        Flowable.create(emitter -> {
            if (authData && authenticationRequest.getIdentity() == username
                    && authenticationRequest.getSecret() == authData.secret) {
                emitter.onNext(new UserDetails(username, authData.roles as List))
                emitter.onComplete()
            } else {
                log.info "${username} caused an Auth Exception"
                emitter.onError(new AuthenticationException(new AuthenticationFailed("Authentication Failed for ${username}")))
            }
        }, BackpressureStrategy.ERROR)
    }
}