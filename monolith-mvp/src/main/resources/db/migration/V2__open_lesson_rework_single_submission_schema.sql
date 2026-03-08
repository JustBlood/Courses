-- TASK-02: single-submission progress model for lessons

-- 1) lesson_submissions: extend to store per-question progress in one JSON payload
alter table lesson_submissions rename column passed to completed;

alter table lesson_submissions
    add column question_progress_json jsonb;

alter table lesson_submissions
    add column attempt_counter integer not null default 0;

update lesson_submissions
set completed = case
    when status in ('COMPLETE', 'INCOMPLETE') then true
    else false
end;

-- 2) one actual submission per student+lesson
-- NOTE: no deduplication is performed intentionally; migration should fail on duplicates.
create unique index if not exists uk_lesson_submissions_lesson_student
    on lesson_submissions (lesson_id, student_id);

-- 3) remove obsolete review/history tables from old flow
drop table if exists submission_question_reviews;
drop table if exists lesson_submission_status_history;
