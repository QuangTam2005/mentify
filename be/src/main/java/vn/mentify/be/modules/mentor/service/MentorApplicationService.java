package vn.mentify.be.modules.mentor.service;

import vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationSubmissionResponse;
import vn.mentify.be.modules.mentor.dto.response.CurrentMentorApplicationResponse;

import java.util.UUID;

public interface MentorApplicationService {

    MentorApplicationSubmissionResponse submitInitialApplication(UUID userId, SubmitMentorApplicationRequest request);

    MentorApplicationSubmissionResponse resubmitApplication(UUID userId, SubmitMentorApplicationRequest request);

    CurrentMentorApplicationResponse getCurrentApplication(UUID userId);
}
