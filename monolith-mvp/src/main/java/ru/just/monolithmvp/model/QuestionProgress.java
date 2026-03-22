package ru.just.monolithmvp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionProgress {
    private Long questionId;
    private List<String> answers;
    private QuestionPointsType pointsType;
    private OpenReviewStatus reviewStatus;
    private String reviewComment;
}
