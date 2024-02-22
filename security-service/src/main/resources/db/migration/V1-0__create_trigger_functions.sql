CREATE OR REPLACE FUNCTION set_role_id()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.role_id IS NULL THEN
        EXECUTE 'SELECT nextval(''role_id_seq'')' INTO NEW.role_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
--
CREATE OR REPLACE FUNCTION set_user_id()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.user_id IS NULL THEN
        EXECUTE 'SELECT nextval(''user_id_seq'')' INTO NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
--
CREATE OR REPLACE FUNCTION set_random_uuid()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.id IS NULL THEN
        EXECUTE gen_random_uuid();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;