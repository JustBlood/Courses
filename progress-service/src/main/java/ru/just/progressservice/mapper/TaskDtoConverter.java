package ru.just.progressservice.mapper;

import org.springframework.stereotype.Component;
import ru.just.progressservice.dto.LessonDto;
import ru.just.progressservice.dto.TaskDto;
import ru.just.progressservice.dto.UserAnswerDto;

@Component
public class TaskDtoConverter {

    // FIXME: ну тут пиздец, но не похуй ли?
    public TaskDto convertToTaskDto(LessonDto lessonDto, UserAnswerDto userAnswerDto) {
        if (lessonDto instanceof LessonDto.HtmlLessonDto && userAnswerDto instanceof UserAnswerDto.HtmlUserAnswerDto) {
            return new TaskDto.TheoryTaskDto();
        } else if (lessonDto instanceof LessonDto.TestLessonDto testLessonDto
                && userAnswerDto instanceof UserAnswerDto.TestUserAnswerDto testUserAnswerDto) {
            return new TaskDto.TestTaskDto(testUserAnswerDto.getUserAnswer(), testLessonDto.getAnswer());
        } else if (lessonDto instanceof LessonDto.MultiTestLessonDto multiTestLessonDto
                && userAnswerDto instanceof UserAnswerDto.MultiTestUserAnswerDto multiTestUserAnswerDto) {
            return new TaskDto.MultiTestTaskDto(multiTestUserAnswerDto.getUserAnswers(), multiTestLessonDto.getCorrectAnswers());
        } else if (lessonDto instanceof LessonDto.CodeLessonDto codeLessonDto
                && userAnswerDto instanceof UserAnswerDto.CodeUserAnswerDto codeUserAnswerDto) {
            return new TaskDto.CodeTaskDto(codeLessonDto.getCodeTests(), codeUserAnswerDto.getUserCode());
        }
        throw new UnsupportedOperationException("Операция не поддерживается");
    }
}
