package vn.mentify.be.modules.mentor.service;

import vn.mentify.be.modules.mentor.dto.MentorApplicationSubmissionResult;
import vn.mentify.be.modules.mentor.dto.SubmitMentorApplicationCommand;

import java.util.UUID;

public interface MentorApplicationService {

    MentorApplicationSubmissionResult submitInitialApplication(UUID userId, SubmitMentorApplicationCommand command);
}
