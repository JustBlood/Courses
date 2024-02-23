INSERT INTO role(role_id, name)
    VALUES (1, 'ROLE_OWNER'), (2, 'ROLE_ADMIN'), (3, 'ROLE_TEACHER'), (4, 'ROLE_STUDENT');
INSERT INTO users(user_id, login, email, password)
    VALUES(1, 'owner', 'owner@owner.ru', '$2y$10$haE51K5cQX4ZaKzIEmNXO.uW5iLoSETft9Ix6.y2xVd7AbbHge3d2');
INSERT INTO user_role(user_id, role_id)
    VALUES(1,1);
