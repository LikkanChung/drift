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

	FOREIGN KEY (uid) REFERENCES users(uid)
);

CREATE TABLE authenticated_clients (
  uid BIGSERIAL,
  token BYTEA,
  expire TIMESTAMP,

  FOREIGN KEY (uid) REFERENCES users(uid),
  CHECK (octet_length(token) = 64),
  CHECK (expire > NOW())
);