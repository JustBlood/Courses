INSERT INTO role(name)
    VALUES ('ROLE_OWNER'), ('ROLE_ADMIN'), ('ROLE_TEACHER'), ('ROLE_STUDENT');
INSERT INTO users(login, email, password)
    VALUES('owner', 'owner@owner.ru', '$2y$10$haE51K5cQX4ZaKzIEmNXO.uW5iLoSETft9Ix6.y2xVd7AbbHge3d2');
INSERT INTO user_role(user_id, role_id)
    VALUES(1,1);
