package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
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
import vn.mentify.be.modules.auth.persistence.UserIdentityRepository;
import vn.mentify.be.modules.auth.service.AuthServiceImpl;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.shared.api.ApiException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private AccountActionTokenRepository tokenRepository;

    @Mock
    private EmailSender emailSender;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(tokenRepository.save(any(AccountActionTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new AuthServiceImpl(
                userRepository,
                userProfileRepository,
                userIdentityRepository,
                tokenRepository,
                new BCryptPasswordEncoder(4),
                emailSender,
                Clock.fixed(NOW, ZoneOffset.UTC),
                "https://app.mentify.vn");
    }

    @Test
    void rejectsGoogleIdentityWithoutVerifiedEmail() {
        DefaultOAuth2User oauthUser = googleUser(false);

        assertThatThrownBy(() -> service.processOAuth2Login("google", oauthUser))
                .isInstanceOf(ApiException.class)
                .hasMessage("Google phải cung cấp subject, email và email đã xác minh");
    }

    @Test
    void doesNotAutoLinkGoogleIdentityToExistingLocalAccount() {
        UserEntity localUser = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(localUser));
        when(userIdentityRepository.findByProviderAndProviderSubject("google", "google-sub-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processOAuth2Login("google", googleUser(true)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Google account cần được liên kết rõ ràng với tài khoản hiện tại");
    }

    @Test
    void setsPasswordAndAllowsPasswordLogin() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        service.setOrChangePassword(userId, new SetPasswordRequest(null, "correct horse battery"));

        assertThat(user.hasPassword()).isTrue();
        assertThat(user.getPasswordChangedAt()).isEqualTo(NOW);
        assertThat(service.passwordLogin(new PasswordLoginRequest(
                "OWNER@example.com", "correct horse battery"))).isSameAs(user);
    }

    @Test
    void sendsEmailChangeLinkWithoutChangingCurrentEmail() {
        UserEntity user = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        service.requestEmailChange(UUID.randomUUID(), new EmailChangeRequest("new@example.com", null));

        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().to()).isEqualTo("new@example.com");
        assertThat(messageCaptor.getValue().text()).contains("/confirm-email?token=");
    }

    @Test
    void forgotPasswordForUnknownEmailReturnsWithoutSendingMail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset(new ForgotPasswordRequest("unknown@example.com"));

        verify(emailSender, never()).send(any(EmailMessage.class));
    }

    @Test
    void forgotPasswordEnforcesCooldown() throws Exception {
        UserEntity user = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        setId(user, UUID.randomUUID());
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        AccountActionTokenEntity previous = new AccountActionTokenEntity(
                user,
                AccountActionTokenPurpose.PASSWORD_RESET,
                "previous-hash",
                null,
                NOW.plusSeconds(120));
        setCreatedAt(previous, NOW.minusSeconds(30));
        when(tokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(
                any(UUID.class), eq(AccountActionTokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.of(previous));

        service.requestPasswordReset(new ForgotPasswordRequest("owner@example.com"));

        verify(emailSender, never()).send(any(EmailMessage.class));
        verify(tokenRepository, never()).save(any(AccountActionTokenEntity.class));
    }

    @Test
    void passwordResetTokenCanOnlyBeUsedOnce() {
        UserEntity user = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        service.requestPasswordReset(new ForgotPasswordRequest("owner@example.com"));

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        String emailText = messageCaptor.getValue().text();
        String rawToken = emailText.substring(emailText.lastIndexOf("token=") + "token=".length());
        AccountActionTokenEntity token = new AccountActionTokenEntity(
                user,
                AccountActionTokenPurpose.PASSWORD_RESET,
                "reset-hash",
                null,
                NOW.plusSeconds(1800));
        when(tokenRepository.findByTokenHashAndPurpose(anyString(), eq(AccountActionTokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        service.resetPassword(new PasswordResetRequest(rawToken, "correct horse battery"));

        assertThat(user.hasPassword()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.resetPassword(new PasswordResetRequest(rawToken, "another password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void expiredPasswordResetTokenIsRejected() {
        UserEntity user = new UserEntity("owner@example.com", UserType.UNIVERSITY_STUDENT);
        AccountActionTokenEntity token = new AccountActionTokenEntity(
                user,
                AccountActionTokenPurpose.PASSWORD_RESET,
                "expired-hash",
                null,
                NOW.minusSeconds(1));
        when(tokenRepository.findByTokenHashAndPurpose(anyString(), eq(AccountActionTokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(
                new PasswordResetRequest("expired-token", "correct horse battery")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");
    }

    private static void setCreatedAt(AccountActionTokenEntity token, Instant createdAt) throws Exception {
        var field = token.getClass().getSuperclass().getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(token, createdAt);
    }

    private static void setId(UserEntity user, UUID id) throws Exception {
        var field = UserEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }

    private DefaultOAuth2User googleUser(boolean verified) {
        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", "google-sub-1",
                        "email", "owner@example.com",
                        "email_verified", verified,
                        "name", "Owner"),
                "sub");
    }
}
