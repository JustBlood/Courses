alter table lesson_submission_status_history
    drop constraint if exists fk_submission_status_history_submission;

alter table lesson_submission_status_history
    add constraint fk_submission_status_history_submission
        foreign key (submission_id) references lesson_submissions(id)
        on delete cascade;
