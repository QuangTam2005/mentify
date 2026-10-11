package vn.mentify.be.modules.mentor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDecision;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDetail;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminListItem;
import java.util.UUID;

public interface MentorApplicationAdminService {

    Page<MentorApplicationAdminListItem> listPending(Pageable pageable);

    MentorApplicationAdminDetail getDetails(UUID applicationId);

    MentorApplicationAdminDecision approve(UUID applicationId);

    MentorApplicationAdminDecision reject(UUID applicationId, String rejectionReason);
}
