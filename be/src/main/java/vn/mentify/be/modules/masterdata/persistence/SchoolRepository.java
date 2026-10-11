package vn.mentify.be.modules.masterdata.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;

import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<SchoolEntity, UUID> {

    Optional<SchoolEntity> findByCode(String code);

    @Query("""
        SELECT s FROM SchoolEntity s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:region IS NULL OR lower(s.region) = lower(:region))
          AND (:campusType IS NULL OR s.campusType = :campusType)
          AND (:search IS NULL OR
               lower(s.name) LIKE lower(concat('%', :search, '%')) OR
               lower(s.shortName) LIKE lower(concat('%', :search, '%')) OR
               lower(s.commonAbbreviation) LIKE lower(concat('%', :search, '%')) OR
               lower(s.code) LIKE lower(concat('%', :search, '%')) OR
               lower(s.admissionCode) LIKE lower(concat('%', :search, '%')))
        ORDER BY s.name ASC
    """)
    Page<SchoolEntity> searchSchools(
            @Param("search") String search,
            @Param("region") String region,
            @Param("campusType") CampusType campusType,
            @Param("status") String status,
            Pageable pageable);
}
