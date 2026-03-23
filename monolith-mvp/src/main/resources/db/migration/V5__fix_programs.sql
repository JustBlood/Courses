alter table learning_programs drop column deadline_days;

alter table learning_programs
    add column deadline_days bigint;
