ALTER TABLE public.users
    ADD COLUMN deliver_status varchar(64) NOT NULL DEFAULT 'NOT_SENT'
        CHECK (deliver_status = ANY (ARRAY['NOT_SENT'::varchar,'SENT'::varchar,'DELIVERED'::varchar]::text[]))
