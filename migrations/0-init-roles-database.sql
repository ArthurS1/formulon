CREATE ROLE server WITH LOGIN;
-- Your job to add the password with ALTER ROLE form WITH PASSWORD '';
CREATE DATABASE formulon WITH OWNER server;
-- After this initial script, you should connect to the formulon DATABASE
-- as server to roll out the next scripts.
