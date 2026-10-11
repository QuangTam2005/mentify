package vn.mentify.be.support;

import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static UserEntity saveUser(
            UserRepository repository,
            String email,
            UserType userType,
            UserRole role) {
        UserEntity user = new UserEntity(email, userType);
        user.setRole(role);
        return repository.saveAndFlush(user);
    }
}
