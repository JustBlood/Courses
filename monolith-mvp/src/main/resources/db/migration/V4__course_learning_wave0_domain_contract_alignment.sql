-- Wave 0 (REF-CM-01): domain/contract alignment for course-learning area

-- 1) Drop obsolete course flags/columns
alter table courses
    drop column if exists deadline_at;

alter table courses
    drop column if exists allow_continue_after_fail;

alter table courses
    drop column if exists keep_access_after_deadline;

-- 2) Remove obsolete theory content type column
alter table lessons
    drop column if exists theory_content_type;

-- 3) Practice question type must be non-null
-- Precheck/fix: default null question_type rows to OPEN_ANSWER before adding NOT NULL.
update practice_questions
set question_type = 'OPEN_ANSWER'
where question_type is null;

alter table practice_questions
    alter column question_type set not null;
