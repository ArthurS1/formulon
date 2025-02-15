CREATE ROLE formulon WITH LOGIN;
-- Your job to add the password with ALTER ROLE formulon WITH PASSWORD '';
CREATE DATABASE formulon WITH OWNER formulon;
-- After this initial script, you should connect to the formulon DATABASE
-- as server to roll out the next scripts.
