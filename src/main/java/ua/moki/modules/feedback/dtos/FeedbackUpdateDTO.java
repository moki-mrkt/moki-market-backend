package ua.moki.modules.feedback.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackUpdateDTO (
        @NotBlank(message = "Comment cannot be empty")
        @Size(min = 2, max = 1000, message = "Comment must be between 2 and 1000 characters")
        String comment,
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating
) {
}
