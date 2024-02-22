-- refresh token
CREATE TABLE IF NOT EXISTS public.refresh_token
(
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL DEFAULT now(),
    device_id uuid NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
                                user_id bigint NOT NULL,
                                CONSTRAINT refresh_token_pkey PRIMARY KEY (id),
    CONSTRAINT device_id_user_id_uq UNIQUE (device_id, user_id),
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
    REFERENCES public.users (user_id) MATCH SIMPLE
                            ON UPDATE NO ACTION
                            ON DELETE NO ACTION
    );
CREATE TRIGGER set_refresh_token_id
    BEFORE INSERT ON public.refresh_token
    FOR EACH ROW
    EXECUTE FUNCTION set_random_uuid();