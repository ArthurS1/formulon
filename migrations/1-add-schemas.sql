CREATE TABLE schemas (
  id    uuid NOT NULL PRIMARY KEY,
  name varchar(80) NOT NULL
);
GRANT INSERT, SELECT, UPDATE, DELETE ON schemas TO server;
