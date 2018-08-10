CREATE TABLE users
(id VARCHAR(20) PRIMARY KEY,
 name VARCHAR(128),
 email VARCHAR(128),
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 pass VARCHAR(300));

-- create a new account
-- username: admin; password: admin
insert into users (id, name, admin, last_login, is_active, pass)
values (1, 'admin', true, now(), true, 'bcrypt+sha512$86186fc28f83b3e3db78bcf8350a3a57$12$8f215420e68fd7922561167b07354f05d8db6d49e212689e');