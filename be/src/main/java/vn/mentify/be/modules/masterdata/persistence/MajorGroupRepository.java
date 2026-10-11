package vn.mentify.be.modules.masterdata.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MajorGroupRepository extends JpaRepository<MajorGroupEntity, UUID> {

    Optional<MajorGroupEntity> findByCode(String code);

    @Query("""
        SELECT m FROM MajorGroupEntity m
        WHERE (:isActive IS NULL OR m.isActive = :isActive)
          AND (:search IS NULL OR
               lower(m.name) LIKE lower(concat('%', :search, '%')) OR
               m.code LIKE concat('%', :search, '%') OR
               lower(m.description) LIKE lower(concat('%', :search, '%')))
        ORDER BY m.code ASC
    """)
    List<MajorGroupEntity> searchMajorGroups(
            @Param("search") String search,
            @Param("isActive") Boolean isActive);
}
