alter table lessons
    drop column if exists random_question_count;

alter table lessons
    drop column if exists blocked_during_attempt;

update practice_questions
set options_raw = case
                       when options_raw is null then null
                       else '["' || replace(replace(replace(options_raw, '\\', '\\\\'), '"', '\\"'), ';;', '","') || '"]'
    end;

update practice_questions
set correct_answers_raw = case
                               when correct_answers_raw is null then null
                               else '["' || replace(replace(replace(correct_answers_raw, '\\', '\\\\'), '"', '\\"'), ';;', '","') || '"]'
    end;
