package vn.mentify.be.modules.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.user.dto.RecordConsentRequest;
import vn.mentify.be.modules.user.dto.UpdateProfileRequest;
import vn.mentify.be.modules.user.dto.UserConsentResponse;
import vn.mentify.be.modules.user.dto.UserProfileResponse;
import vn.mentify.be.modules.user.persistence.UserConsentEntity;
import vn.mentify.be.modules.user.persistence.UserConsentRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryEntity;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryRepository;
import vn.mentify.be.shared.api.ResourceNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserConsentRepository userConsentRepository;
    private final UserTypeHistoryRepository userTypeHistoryRepository;
    private final SchoolRepository schoolRepository;
    private final MajorGroupRepository majorGroupRepository;
    private final Clock clock;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserConsentRepository userConsentRepository,
            UserTypeHistoryRepository userTypeHistoryRepository,
            SchoolRepository schoolRepository,
            MajorGroupRepository majorGroupRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userConsentRepository = userConsentRepository;
        this.userTypeHistoryRepository = userTypeHistoryRepository;
        this.schoolRepository = schoolRepository;
        this.majorGroupRepository = majorGroupRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        UserProfileEntity profile = userProfileRepository.findByUserId(userId).orElse(null);
        return UserProfileResponse.from(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (request.newUserType() != null && request.newUserType() != user.getUserType()) {
            UserType oldType = user.getUserType();
            user.promoteUserType(request.newUserType());
            userRepository.save(user);

            Instant now = clock.instant();
            UserTypeHistoryEntity history = new UserTypeHistoryEntity(user, oldType, request.newUserType(), now);
            userTypeHistoryRepository.save(history);
        }

        SchoolEntity school = null;
        if (request.schoolId() != null) {
            school = schoolRepository.findById(request.schoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường với id: " + request.schoolId()));
        }

        MajorGroupEntity majorGroup = null;
        if (request.majorGroupCode() != null && !request.majorGroupCode().isBlank()) {
            majorGroup = majorGroupRepository.findByCode(request.majorGroupCode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm ngành với mã: " + request.majorGroupCode()));
        }

        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> new UserProfileEntity(user));

        profile.update(
                request.fullName(),
                request.bio(),
                request.avatarUrl(),
                school,
                majorGroup,
                request.customMajorName()
        );

        UserProfileEntity savedProfile = userProfileRepository.save(profile);
        return UserProfileResponse.from(user, savedProfile);
    }

    @Override
    @Transactional
    public UserConsentResponse recordConsent(UUID userId, RecordConsentRequest request, String ipAddress, String userAgent) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        OffsetDateTime acceptedAt = OffsetDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);

        UserConsentEntity consent = new UserConsentEntity(
                user,
                request.consentType(),
                request.policyVersion(),
                acceptedAt,
                ipAddress,
                userAgent,
                request.guardianName(),
                request.guardianContact()
        );

        UserConsentEntity saved = userConsentRepository.save(consent);
        return UserConsentResponse.from(saved);
    }
}
