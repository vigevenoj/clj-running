-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, name, email, is_active, last_login, pass)
VALUES (:id,
        :name,
        :email,
        :is-active,
        --~ (if (= :postgresql (:db-type params)) "(now() at time zone 'utc')," "now(),")
        :pass)

-- :name update-user! :! :1
-- :doc updates an existing user record, but not their password
UPDATE users
SET name = :name,
    email = :email,
    admin = :admin,
    is_active = :is-active
    last_login= --~ (if (= :postgresql (:db-type params)) "(now() at time zone 'utc')" "now()")
WHERE id = :id

-- :name update-user-with-pass! :! :1
-- :doc updates all fields of an existing user
UPDATE users
SET name = :name,
    email = :email,
    admin = :admin,
    is_active = :is-active,
    pass = :pass,
    last_login = --~ (if (= :postgresql (:db-type params)) "(now() at time zone 'utc')" "now()")
WHERE id = :id;

-- :name get-user-by-name :? :1
-- :doc get a user by their username
SELECT id, name, email, admin, last_login, is_active, pass
FROM users
WHERE name = :name

-- :name get-users-by-name :? :*
-- :doc get users by partial username match
SELECT id, name, email, admin, last_login, is_active, pass
FROM users
where name ilike :name
order by name

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT id, name, email, admin, last_login, is_active, pass
FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name delete-user-by-name! :! :n
-- :doc delete a user by their name
DELETE FROM users
WHERE name = :name