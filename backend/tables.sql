/*Database that the backend runs on?*/

DROP TABLE IF EXISTS Users, Alarms;

CREATE TABLE users (
	uid BIGSERIAL,
	preferred_name VARCHAR(40),

	PRIMARY KEY(uid)
);

CREATE TABLE alarms (
	aid BIGSERIAL,
	uid BIGSERIAL,
	time TIMESTAMP,

	FOREIGN KEY (uid) REFERENCES Users(uid)
);