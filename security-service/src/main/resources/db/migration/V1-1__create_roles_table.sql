-- roles
CREATE SEQUENCE IF NOT EXISTS public.role_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE IF NOT EXISTS public.role
(
    role_id bigint NOT NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT role_pkey PRIMARY KEY (role_id),
    CONSTRAINT role_name_uq UNIQUE (name)
    );
CREATE TRIGGER set_role_id_trigger
    BEFORE INSERT ON public.role
    FOR EACH ROW
    EXECUTE FUNCTION set_role_id();