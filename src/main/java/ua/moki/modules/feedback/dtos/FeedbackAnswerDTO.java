package ua.moki.modules.feedback.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackAnswerDTO(
        @NotBlank(message = "Answer cannot be empty")
        @Size(min = 2, max = 1000, message = "Answer must be between 2 and 1000 characters")
        String answer
) {
}
