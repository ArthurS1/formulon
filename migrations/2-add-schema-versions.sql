ALTER TABLE schemas ADD active_schema_id uuid;
CREATE TABLE schema_versions (
  id        uuid PRIMARY KEY,
  date      timestamp NOT NULL,
  schema_id uuid REFERENCES schemas
);
