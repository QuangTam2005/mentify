package vn.mentify.be.modules.mentor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationSubmissionResponse;
import vn.mentify.be.modules.mentor.dto.response.CurrentMentorApplicationResponse;
import vn.mentify.be.modules.mentor.service.MentorApplicationService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.service.CurrentUserResolver;
import vn.mentify.be.shared.api.ApiSuccessResponse;

@RestController
@RequestMapping({"/api/v1/mentor", "/api/mentor"})
public class MentorApplicationController {

    private final MentorApplicationService mentorApplicationService;
    private final CurrentUserResolver currentUserResolver;
    private final SuccessResponseFactory responseFactory;

    public MentorApplicationController(
            MentorApplicationService mentorApplicationService,
            CurrentUserResolver currentUserResolver,
            SuccessResponseFactory responseFactory) {
        this.mentorApplicationService = mentorApplicationService;
        this.currentUserResolver = currentUserResolver;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccessResponse<MentorApplicationSubmissionResponse> submitInitialApplication(
            @Valid @RequestBody SubmitMentorApplicationRequest requestDto,
            HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        MentorApplicationSubmissionResponse response =
                mentorApplicationService.submitInitialApplication(currentUser.getId(), requestDto);
        return responseFactory.success(response, request);
    }

    @GetMapping("/applications/me")
    public ApiSuccessResponse<CurrentMentorApplicationResponse> getCurrentApplication(HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        CurrentMentorApplicationResponse response = mentorApplicationService.getCurrentApplication(currentUser.getId());
        return responseFactory.success(response, request);
    }
}
