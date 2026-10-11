package vn.mentify.be.modules.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenEntity;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenPurpose;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class OAuth2LoginCodeService {

    private static final Duration CODE_TTL = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountActionTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public OAuth2LoginCodeService(
            AccountActionTokenRepository tokenRepository,
            UserRepository userRepository,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public String issue(UserEntity user) {
        String rawCode = newCode();
        Instant now = clock.instant();
        tokenRepository.save(new AccountActionTokenEntity(
                user,
                AccountActionTokenPurpose.OAUTH2_LOGIN,
                hash(rawCode),
                null,
                now.plus(CODE_TTL)));
        return rawCode;
    }

    @Transactional
    public UserEntity consume(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw invalidCode();
        }
        AccountActionTokenEntity token = tokenRepository
                .findByTokenHashAndPurposeForUpdate(hash(rawCode), AccountActionTokenPurpose.OAUTH2_LOGIN)
                .orElseThrow(this::invalidCode);
        Instant now = clock.instant();
        if (!token.isUsableAt(now)) {
            throw invalidCode();
        }
        token.markUsed(now);
        return userRepository.findById(token.getUser().getId())
                .orElseThrow(this::invalidCode);
    }

    private static String newCode() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawCode.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo mã đăng nhập OAuth2", exception);
        }
    }

    private ApiException invalidCode() {
        return new ApiException(ApiErrorCode.UNAUTHENTICATED, "Mã đăng nhập OAuth2 không hợp lệ hoặc đã hết hạn");
    }
}
