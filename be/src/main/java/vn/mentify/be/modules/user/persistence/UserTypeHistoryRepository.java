package vn.mentify.be.modules.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTypeHistoryRepository extends JpaRepository<UserTypeHistoryEntity, UUID> {

    List<UserTypeHistoryEntity> findByUserIdOrderByChangedAtDesc(UUID userId);
}
