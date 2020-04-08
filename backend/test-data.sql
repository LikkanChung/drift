INSERT INTO users (preferred_name, username, password)
VALUES ('Jack', 'jack1999', 'letmein0');

INSERT INTO alarms (uid, time)
SELECT users.uid, TIMESTAMP '2020-03-03 08:30'
FROM users
WHERE users.preferred_name = 'Jack';

INSERT INTO alarms (uid, time)
SELECT users.uid, TIMESTAMP '2020-03-03 10:45'
FROM users
WHERE users.preferred_name = 'Jack';

INSERT INTO alarms (uid, time)
SELECT users.uid, TIMESTAMP '2020-02-20 11:23'
FROM users
WHERE users.preferred_name = 'Jack';