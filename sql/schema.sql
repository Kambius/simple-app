DROP TABLE IF EXISTS users;

CREATE TABLE users (
    email      TEXT NOT NULL,
    id         INT NOT NULL,
    first_name TEXT NOT NULL,
    last_name  TEXT NOT NULL,
    PRIMARY KEY (email)
);