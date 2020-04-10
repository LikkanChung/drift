/*Database that the backend runs on*/

DROP TABLE IF EXISTS users, alarms, authenticated_clients, pairing_transactions;

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
  expire TIMESTAMP WITH TIME ZONE,

  FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE,
  CHECK (octet_length(token) = 64),
  UNIQUE(token),
  CHECK (expire > NOW())
);

CREATE TABLE pairing_transactions (
    syn_code CHAR(4),
    uid BIGSERIAL,
    access_level INTEGER,
    ack_code CHAR(4) DEFAULT NULL,
    expire TIMESTAMP WITH TIME ZONE,
    auth_token BYTEA DEFAULT NULL,
    token_expire TIMESTAMP WITH TIME ZONE DEFAULT NULL,

    PRIMARY KEY(syn_code),
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE,
    CHECK (access_level NOTNULL AND access_level > -2),
    CHECK (expire > NOW()),
    CHECK (token_expire IS NULL OR token_expire > NOW())
);