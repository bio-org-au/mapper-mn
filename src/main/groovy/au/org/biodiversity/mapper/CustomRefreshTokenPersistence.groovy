package au.org.biodiversity.mapper

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent;
import io.micronaut.security.token.refresh.RefreshTokenPersistence;
import io.micronaut.security.errors.OauthErrorResponseException;
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Optional;

@Singleton
public class CustomRefreshTokenPersistence implements RefreshTokenPersistence {
    private final RefreshTokenRepository refreshTokenRepository;

    public CustomRefreshTokenPersistence(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @EventListener
    public void persistToken(RefreshTokenGeneratedEvent event) {
        if (event != null &&
                event.getRefreshToken() != null &&
                event.getUserDetails() != null &&
                event.getUserDetails().getUsername() != null) {
            String payload = event.getRefreshToken();
            refreshTokenRepository.save(event.getUserDetails() .getUsername(), payload, Boolean.FALSE);
        }
    }

    @Override
    public Publisher<UserDetails> getUserDetails(String refreshToken) {
        return Flowable.create(emitter -> {
            Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
            if (tokenOpt.isPresent()) {
                RefreshTokenEntity token = tokenOpt.get();
                if (token.getRevoked()) {
                    emitter.onError(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token revoked", null));
                } else {
                    emitter.onNext(new UserDetails(token.getUsername(), new ArrayList<>()));
                    emitter.onComplete();
                }
            } else {
                emitter.onError(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token not found", null));
            }
        }, BackpressureStrategy.ERROR);

    }
}