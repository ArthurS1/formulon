CREATE TABLE responses (
  id                uuid PRIMARY KEY,
  schema_version_id uuid REFERENCES schema_version NOT NULL,
  content           jsonb NOT NULL
);
