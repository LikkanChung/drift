/*Database that the backend runs on?*/

DROP TABLE IF EXISTS users, alarms, authenticated_clients;

CREATE TABLE users (
	uid BIGSERIAL,
	preferred_name VARCHAR(40) NOT NULL,
	username VARCHAR(20) NOT NULL,
	password VARCHAR(30) NOT NULL,

	PRIMARY KEY(uid),
	UNIQUE(username),
	CHECK (char_length(password) >= 8)
);

CREATE TABLE alarms (
	aid BIGSERIAL,
	uid BIGSERIAL,
	time TIMESTAMP,

	FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE authenticated_clients (
  uid BIGSERIAL,
  token BYTEA,
  access_level INTEGER,
  expire TIMESTAMP,

  FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE,
  CHECK (octet_length(token) = 64),
  UNIQUE(token),
  CHECK (expire > NOW())
);