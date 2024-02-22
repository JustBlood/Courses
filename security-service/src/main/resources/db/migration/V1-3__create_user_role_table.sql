-- user_role
CREATE TABLE IF NOT EXISTS public.user_role
(
    role_id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT role_user_pkey PRIMARY KEY (role_id, user_id),
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
    REFERENCES public.users (user_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT role_id_fkey FOREIGN KEY (role_id)
    REFERENCES public.role (role_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    );