package ua.moki.modules.feedback.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.moki.modules.feedback.dtos.*;
import ua.moki.modules.feedback.services.FeedbackService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponseDTO> createFeedback(Principal principal, @RequestBody @Valid FeedbackRequestDTO dto) {

        FeedbackResponseDTO responseDTO = feedbackService.createFeedback(UUID.fromString(principal.getName()), dto);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponseDTO> updateFeedback(Authentication authentication,
                                                              @PathVariable Long id,
                                                              @RequestBody @Valid FeedbackUpdateDTO dto) {

        FeedbackResponseDTO responseDTO = feedbackService.updateFeedback(id, dto, authentication);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{id}/answer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponseDTO> addAnswerToFeedback(Authentication authentication,
                                                              @PathVariable Long id,
                                                              @RequestBody @Valid FeedbackAnswerDTO dto) {

        String userRole = authentication.getAuthorities().iterator().next().getAuthority();

        FeedbackResponseDTO responseDTO = feedbackService.addAnswerToFeedback(id, userRole, dto);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id, Authentication authentication) {

        feedbackService.deleteFeedback(id, authentication);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FeedbackResponseDTO>> getFeedbacksByUserId(Principal principal,
                                                               @RequestParam @Min(0) int page,
                                                               @RequestParam @Min(0) int size) {

        Page<FeedbackResponseDTO> responseDTO = feedbackService.getFeedbacksByUserId(UUID.fromString(principal.getName()), page, size);

        return ResponseEntity.ok(responseDTO);
    }


    @GetMapping("/product/{productId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<FeedbackResponseDTO>> getFeedbacksByProductId(@PathVariable Long productId,
                                                                             @RequestParam @Min(0) int page,
                                                                             @RequestParam @Min(0) int size) {

        Page<FeedbackResponseDTO> responseDTO = feedbackService.getFeedbacksByProductId(productId, page, size);

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/store")
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<FeedbackStoreResponseDTO> getFeedbacksByStore(@RequestParam @Min(0) int page,
                                                                        @RequestParam @Min(0) int size) {

        Page<FeedbackResponseDTO> responseDTO = feedbackService.getFeedbacksAboutStore(page, size);

        return ResponseEntity.ok(new FeedbackStoreResponseDTO(
                feedbackService.getAverageRatingForStore(),
                responseDTO
        ));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<FeedbackStoreResponseDTO> getAllFeedbacks(@RequestParam @Min(0) int page,
                                                                    @RequestParam @Min(0) int size) {

        Page<FeedbackResponseDTO> responseDTO = feedbackService.getAllFeedbacks(page, size);

        return ResponseEntity.ok(new FeedbackStoreResponseDTO(
                feedbackService.getAverageRatingForStore(),
                responseDTO
        ));
    }
}
