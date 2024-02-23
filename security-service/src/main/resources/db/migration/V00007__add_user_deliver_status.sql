ALTER TABLE public.users
    ADD COLUMN user_deliver_status varchar(64) NOT NULL DEFAULT 'NOT_SENT'
        CHECK (user_deliver_status = ANY (ARRAY['NOT_SENT'::varchar,'SENT'::varchar,'DELIVERED'::varchar]::text[]))
