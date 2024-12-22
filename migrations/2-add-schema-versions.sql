ALTER TABLE schemas ADD active_schema_id uuid;
CREATE TABLE schema_versions (
  id        uuid NOT NULL PRIMARY KEY,
  date      timestamp NOT NULL,
  schema_id uuid NOT NULL
);
GRANT INSERT, SELECT, UPDATE, DELETE ON schema_versions TO server;

