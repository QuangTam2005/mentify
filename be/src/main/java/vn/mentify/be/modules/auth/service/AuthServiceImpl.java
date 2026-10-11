package vn.mentify.be.modules.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.auth.domain.MentorStatus;
import vn.mentify.be.modules.auth.dto.AuthMeResponse;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.auth.dto.EmailChangeRequest;
import vn.mentify.be.modules.auth.dto.ForgotPasswordRequest;
import vn.mentify.be.modules.auth.dto.PasswordLoginRequest;
import vn.mentify.be.modules.auth.dto.PasswordResetRequest;
import vn.mentify.be.modules.auth.dto.SetPasswordRequest;
import vn.mentify.be.modules.auth.mail.EmailMessage;
import vn.mentify.be.modules.auth.mail.EmailSender;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenEntity;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenPurpose;
import vn.mentify.be.modules.auth.persistence.AccountActionTokenRepository;
import vn.mentify.be.modules.auth.persistence.UserIdentityEntity;
import vn.mentify.be.modules.auth.persistence.UserIdentityRepository;
import vn.mentify.be.modules.user.persistence.AccountStatus;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final Duration EMAIL_CHANGE_TTL = Duration.ofMinutes(30);
    private static final Duration RESET_REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Clock clock;
    private final String mailBaseUrl;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserIdentityRepository userIdentityRepository,
            AccountActionTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            Clock clock,
            @Value("${app.mail.base-url:http://localhost:3000}") String mailBaseUrl) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.clock = clock;
        this.mailBaseUrl = mailBaseUrl;
    }

    @Override
    @Transactional
    public UserEntity processOAuth2Login(String provider, OAuth2User oauth2User) {
        OAuthIdentity identity = readOAuthIdentity(provider, oauth2User);
        Instant now = clock.instant();
        UserIdentityEntity existingIdentity = userIdentityRepository
                .findByProviderAndProviderSubject(identity.provider(), identity.subject())
                .orElse(null);
        UserEntity user;
        if (existingIdentity != null) {
            user = existingIdentity.getUser();
            ensureLoginAllowed(user);
            existingIdentity.recordLogin(identity.email(), true, now);
        } else {
            if (userRepository.findByEmail(identity.email()).isPresent()) {
                throw new ApiException(ApiErrorCode.CONFLICT,
                        "Google account cần được liên kết rõ ràng với tài khoản hiện tại");
            }
            user = new UserEntity(identity.email(), UserType.SCHOOL_STUDENT);
            user.markEmailVerified(now);
            user = userRepository.save(user);
            userIdentityRepository.save(new UserIdentityEntity(
                    user, identity.provider(), identity.subject(), identity.email(), true, now));
        }
        updateProfileFromGoogle(user, oauth2User);
        return user;
    }

    @Override
    @Transactional
    public UserEntity linkOAuth2Identity(String provider, OAuth2User oauth2User, UserEntity currentUser) {
        ensureLoginAllowed(currentUser);
        OAuthIdentity identity = readOAuthIdentity(provider, oauth2User);
        UserIdentityEntity existing = userIdentityRepository
                .findByProviderAndProviderSubject(identity.provider(), identity.subject())
                .orElse(null);
        if (existing != null && !existing.getUser().getId().equals(currentUser.getId())) {
            throw new ApiException(ApiErrorCode.CONFLICT, "Google account đã được liên kết với tài khoản khác");
        }
        if (existing == null) {
            userIdentityRepository.save(new UserIdentityEntity(
                    currentUser, identity.provider(), identity.subject(), identity.email(), true, clock.instant()));
        }
        return currentUser;
    }

    @Override
    @Transactional
    public UserEntity devLogin(DevLoginRequest request) {
        String email = request != null && request.email() != null && !request.email().isBlank()
                ? normalizeEmail(request.email()) : "dev@mentify.vn";
        UserType userType = request != null && request.userType() != null
                ? request.userType() : UserType.UNIVERSITY_STUDENT;
        UserRole role = request != null && request.role() != null ? request.role() : UserRole.USER;
        String fullName = request != null && request.fullName() != null && !request.fullName().isBlank()
                ? request.fullName() : "Dev User";

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.save(new UserEntity(email, userType));
        } else {
            user.setDevUserType(userType);
        }
        user.setRole(role);
        user.markEmailVerified(clock.instant());
        user = userRepository.save(user);

        UserEntity currentUser = user;
        UserProfileEntity profile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> new UserProfileEntity(currentUser));
        profile.update(fullName, profile.getBio(), profile.getAvatarUrl(), profile.getSchool(),
                profile.getMajorGroup(), profile.getCustomMajorName());
        userProfileRepository.save(profile);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity passwordLogin(PasswordLoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.hasPassword() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Email hoặc mật khẩu không đúng");
        }
        ensureLoginAllowed(user);
        return user;
    }

    @Override
    @Transactional
    public void setOrChangePassword(UUID userId, SetPasswordRequest request) {
        UserEntity user = getUser(userId);
        if (user.hasPassword() && (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Mật khẩu hiện tại không đúng");
        }
        validatePassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()), clock.instant());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null || user.getAccountStatus() != AccountStatus.ACTIVE) {
            return;
        }
        Instant now = clock.instant();
        AccountActionTokenEntity previous = tokenRepository
                .findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), AccountActionTokenPurpose.PASSWORD_RESET)
                .orElse(null);
        if (previous != null && previous.getCreatedAt() != null
                && previous.getCreatedAt().plus(RESET_REQUEST_COOLDOWN).isAfter(now)) {
            return;
        }
        if (previous != null && previous.getUsedAt() == null) {
            previous.markUsed(now);
        }
        String rawToken = newToken();
        tokenRepository.save(new AccountActionTokenEntity(user, AccountActionTokenPurpose.PASSWORD_RESET,
                hashToken(rawToken), null, now.plus(PASSWORD_RESET_TTL)));
        emailSender.send(new EmailMessage(user.getEmail(), "Đặt lại mật khẩu Mentify",
                "Mở liên kết sau để đặt lại mật khẩu: " + mailBaseUrl + "/reset-password?token=" + rawToken));
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        Instant now = clock.instant();
        AccountActionTokenEntity token = tokenRepository
                .findByTokenHashAndPurpose(hashToken(request.token()), AccountActionTokenPurpose.PASSWORD_RESET)
                .orElseThrow(this::invalidToken);
        if (!token.isUsableAt(now)) {
            throw invalidToken();
        }
        validatePassword(request.newPassword());
        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()), now);
        token.markUsed(now);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestEmailChange(UUID userId, EmailChangeRequest request) {
        UserEntity user = getUser(userId);
        String newEmail = normalizeEmail(request.newEmail());
        if (newEmail.equals(user.getEmail())) {
            throw new ApiException(ApiErrorCode.CONFLICT, "Email mới phải khác email hiện tại");
        }
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new ApiException(ApiErrorCode.CONFLICT, "Email mới đã được sử dụng");
        }
        if (user.hasPassword() && (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Mật khẩu hiện tại không đúng");
        }
        Instant now = clock.instant();
        String rawToken = newToken();
        tokenRepository.save(new AccountActionTokenEntity(user, AccountActionTokenPurpose.EMAIL_CHANGE,
                hashToken(rawToken), newEmail, now.plus(EMAIL_CHANGE_TTL)));
        emailSender.send(new EmailMessage(newEmail, "Xác minh email Mentify",
                "Mở liên kết sau để xác minh email: " + mailBaseUrl + "/confirm-email?token=" + rawToken));
    }

    @Override
    @Transactional
    public void confirmEmailChange(String rawToken) {
        Instant now = clock.instant();
        AccountActionTokenEntity token = tokenRepository
                .findByTokenHashAndPurpose(hashToken(rawToken), AccountActionTokenPurpose.EMAIL_CHANGE)
                .orElseThrow(this::invalidToken);
        if (!token.isUsableAt(now) || token.getRequestedEmail() == null) {
            throw invalidToken();
        }
        UserEntity user = token.getUser();
        UserEntity collision = userRepository.findByEmail(token.getRequestedEmail()).orElse(null);
        if (collision != null && !collision.getId().equals(user.getId())) {
            throw new ApiException(ApiErrorCode.CONFLICT, "Email mới đã được sử dụng");
        }
        user.changeEmail(token.getRequestedEmail());
        user.markEmailVerified(now);
        token.markUsed(now);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthMeResponse getCurrentUserMe(UserEntity user) {
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return AuthMeResponse.from(user, profile, MentorStatus.NOT_APPLIED, false);
    }

    private UserEntity getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private void ensureLoginAllowed(UserEntity user) {
        if (!user.canLogin()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Tài khoản không thể đăng nhập");
        }
    }

    private OAuthIdentity readOAuthIdentity(String provider, OAuth2User oauth2User) {
        if (provider == null || provider.isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "Provider OAuth không hợp lệ");
        }
        String subject = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        Object verified = oauth2User.getAttribute("email_verified");
        if (subject == null || subject.isBlank() || email == null || email.isBlank() || !isTrue(verified)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                    "Google phải cung cấp subject, email và email đã xác minh");
        }
        return new OAuthIdentity(provider.toLowerCase(Locale.ROOT), subject, normalizeEmail(email));
    }

    private void updateProfileFromGoogle(UserEntity user, OAuth2User oauth2User) {
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserProfileEntity(user));
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        profile.update(profile.getFullName() == null || profile.getFullName().isBlank() ? name : profile.getFullName(),
                profile.getBio(), profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank() ? picture : profile.getAvatarUrl(),
                profile.getSchool(), profile.getMajorGroup(), profile.getCustomMajorName());
        userProfileRepository.save(profile);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "Mật khẩu phải có từ 12 đến 128 ký tự");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo token bảo mật", exception);
        }
    }

    private ApiException invalidToken() {
        return new ApiException(ApiErrorCode.VALIDATION_ERROR, "Token không hợp lệ hoặc đã hết hạn");
    }

    private boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private record OAuthIdentity(String provider, String subject, String email) {
    }
}
