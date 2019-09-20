package au.org.biodiversity.mapper

import io.micronaut.context.annotation.Property
import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.UserDetails
import io.reactivex.Flowable
import org.reactivestreams.Publisher

import javax.inject.Singleton

/**
 * User: pmcneil
 * Date: 12/9/19
 *
 */
@Singleton
class AuthenticationProviderUserPassword implements AuthenticationProvider {

    @Property(name = 'mapper.auth')
    Map authMap

    @Override
    Publisher<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
        String username = authenticationRequest.getIdentity()
        Map authData = authMap[username] as Map
        if ( authData && authenticationRequest.getSecret().equals(authData.secret) ) {
            return Flowable.just(new UserDetails((String) authenticationRequest.getIdentity(), authData.roles as List))
        }
        return Flowable.just(new AuthenticationFailed())
    }
}
