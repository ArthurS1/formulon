CREATE TABLE answers (
  id                uuid PRIMARY KEY,
  schema_version_id uuid REFERENCES schema_versions NOT NULL,
  content           jsonb NOT NULL
);
