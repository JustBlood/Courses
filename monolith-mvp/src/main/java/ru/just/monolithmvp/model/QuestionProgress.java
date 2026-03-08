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
    private Integer questionIndex;
    private List<String> answers;
    private QuestionPointsType pointsType;
    private Integer awardedPoints;
    private OpenReviewStatus reviewStatus;
    private String reviewComment;
}
