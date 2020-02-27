/*Database that the backend runs on?*/

DROP TABLE IF EXISTS Users, Alarms;

CREATE TABLE Users (
	uid SERIAL,

	PRIMARY KEY(uid)
);

CREATE TABLE Alarms (
	aid SERIAL,
	uid INTEGER,
	time TIMESTAMP,

	FOREIGN KEY (uid) REFERENCES Users(uid)
);