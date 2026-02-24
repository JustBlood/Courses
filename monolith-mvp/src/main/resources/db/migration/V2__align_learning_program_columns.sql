alter table learning_programs
    add column if not exists deadline_at timestamp;

alter table learning_programs
    add column if not exists block_after_deadline boolean not null default false;