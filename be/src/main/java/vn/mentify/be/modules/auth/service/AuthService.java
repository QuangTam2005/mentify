package vn.mentify.be.modules.auth.service;

import org.springframework.security.oauth2.core.user.OAuth2User;
import vn.mentify.be.modules.auth.dto.EmailChangeRequest;
import vn.mentify.be.modules.auth.dto.ForgotPasswordRequest;
import vn.mentify.be.modules.auth.dto.PasswordLoginRequest;
import vn.mentify.be.modules.auth.dto.PasswordResetRequest;
import vn.mentify.be.modules.auth.dto.SetPasswordRequest;
import vn.mentify.be.modules.auth.dto.AuthMeResponse;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.user.persistence.UserEntity;

import java.util.UUID;

public interface AuthService {

    UserEntity processOAuth2Login(String provider, OAuth2User oauth2User);

    UserEntity linkOAuth2Identity(String provider, OAuth2User oauth2User, UserEntity currentUser);

    UserEntity devLogin(DevLoginRequest request);

    UserEntity passwordLogin(PasswordLoginRequest request);

    void setOrChangePassword(UUID userId, SetPasswordRequest request);

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(PasswordResetRequest request);

    void requestEmailChange(UUID userId, EmailChangeRequest request);

    void confirmEmailChange(String token);

    AuthMeResponse getCurrentUserMe(UserEntity user);
}
