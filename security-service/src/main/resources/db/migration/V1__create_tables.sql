CREATE SEQUENCE IF NOT EXISTS roles_roles_id_seq INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

CREATE TABLE IF NOT EXISTS public.roles
(
    role_id bigint NOT NULL DEFAULT NEXTVAL('roles_roles_id_seq'),
    name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (role_id),
    CONSTRAINT role_name_uq UNIQUE (name)
);

CREATE SEQUENCE IF NOT EXISTS user_user_id_seq INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

CREATE TABLE IF NOT EXISTS public.users
(
    user_id bigint NOT NULL DEFAULT NEXTVAL('user_user_id_seq'),
    email character varying(255) COLLATE pg_catalog."default",
    password character varying(255) COLLATE pg_catalog."default" NOT NULL,
    username character varying(128) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_username_uq UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS public.roles_user_principal
(
    roles_role_id bigint NOT NULL,
    user_principal_user_id bigint NOT NULL,
    CONSTRAINT roles_user_principal_pkey PRIMARY KEY (roles_role_id, user_principal_user_id),
    CONSTRAINT roles_user_principal_user_id_fkey FOREIGN KEY (user_principal_user_id)
        REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE ,
    CONSTRAINT roles_user_principal_role_id_fkey FOREIGN KEY (roles_role_id)
        REFERENCES public.roles (role_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
