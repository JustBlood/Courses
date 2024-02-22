-- user
CREATE SEQUENCE IF NOT EXISTS public.user_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
CREATE TABLE IF NOT EXISTS public.users
(
    user_id bigint NOT NULL,
    email character varying(255),
    password character varying(255) NOT NULL,
    login character varying(255) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_username_uq UNIQUE (username)
    );
CREATE TRIGGER set_user_id_trigger
    BEFORE INSERT ON public.role
    FOR EACH ROW
    EXECUTE FUNCTION set_user_id();