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

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.refresh.RefreshTokenPersistence
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher

import javax.inject.Singleton

@Slf4j
@Singleton
class CustomRefreshTokenPersistence implements RefreshTokenPersistence {
    /**
     * Author: Mo Ziauddin
     * Handle the refresh token workflow so the refresh token is generated
     * and stored on local file system as part of ugrade to MN 2.3.0
     */

    @Property(name = 'mapper.auth')
    Map authMap

    static String filename = 'refresh-tokens.txt'
    File f = new File(filename)

    CustomRefreshTokenPersistence() {
        // Create the file it not exists
        f.write('')
    }
    @Override
    @EventListener
    void persistToken(RefreshTokenGeneratedEvent event) {
        if(event && event.getRefreshToken() && event.getUserDetails().getUsername()) {
            List allTokens = readStoredRefreshTokens()
            String username = event.getUserDetails().getUsername()
            String newToken = username + '-->' + event.getRefreshToken()
            String myTokenString = allTokens.find {e -> e.contains(username)}
            if (myTokenString) {
                allTokens[allTokens.indexOf(myTokenString)] = newToken
            } else {
                allTokens << newToken
            }
            writeRefreshToken(allTokens)
            log.info "Added new token $newToken and saved to the file system"
        }
    }

    @Override
    Publisher<UserDetails> getUserDetails(String refreshToken) {
        return Flowable.create(emitter -> {
            List allTokens = readStoredRefreshTokens()
            String username = ''
            String refToken = ''
            for (item in allTokens) {
                if (item) {
                    def (un, rt) = item.split('-->')
                    if (rt == refreshToken) {
                        username = un
                        refToken = rt
                    }
                }
            }
            if (refToken) {
                Map authData = authMap[username] as Map
                emitter.onNext(new UserDetails(username, authData.roles as List))
                emitter.onComplete()
            } else {
                emitter.onError(new OauthErrorResponseException(
                        IssuingAnAccessTokenErrorCode.UNAUTHORIZED_CLIENT,
                        "Refresh Token not in the list of saved tokens.", null))
            }
        }, BackpressureStrategy.ERROR)
    }

    void writeRefreshToken(List list) {
        f.setText(list.toString())
    }

    List readStoredRefreshTokens() {
        String fileContent = f.getText('UTF-8')
        List tokens = fileContent.tokenize(',[]')
        return tokens
    }
}