alter table users
    add column if not exists activated boolean not null default true;

update users
set activated = true
where activated is null;
