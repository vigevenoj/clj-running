-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(name, email, last_login pass)
VALUES (:name,
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
WHERE user_id = :user-id

-- :name update-user-with-pass! :! :1
-- :doc updates all fields of an existing user
UPDATE users
SET name = :name,
    email = :email,
    admin = :admin,
    is_active = :is-active,
    pass = :pass,
    last_login = --~ (if (= :postgresql (:db-type params)) "(now() at time zone 'utc')" "now()")
WHERE user_id = :user-id;

-- :name get-user-by-name :? :1
-- :doc get a user by their username
SELECT user_id, name, email, admin, last_login, is_active, pass
FROM users
WHERE name = :name

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT user_id, name, email, admin, last_login, is_active, pass
FROM users
WHERE user_id = :user-id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE user_id = :user-id

-- :name create-run! :! :n
-- :doc create a new run
INSERT INTO runs
(rdate, timeofday, distance, units, elapsed, effort, comment, shoeid)
VALUES (:rdate, :timeofday, :distance, :units, :elapsed, :effort, :comment, :shoeid)

-- :name update-run! :! :n
-- :doc update an existing run
UPDATE runs
SET rdate = :rdate, timeofday = :timeofday, distance = :distance,
 units = :units, elapsed = :elapsed, effort = :effort, commment = :comment, shoeid = :shoeid
WHERE runid = :runid

-- :name delete-run! :! :n
-- :doc deletes a run given the id
DELETE FROM runs
WHERE runid = :runid

-- :name get-runs :? :*
-- :doc retrieves all runs
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs

-- :name get-run :? :1
-- :doc retrieve a run given the id
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE runid = :runid

-- :name get-recent-runs :? :*
-- :doc retrieve runs as recently as the past :limit days
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE rdate >= current_date - :limit::interval
ORDER BY rdate desc

-- :name get-runs-by-date :? :*
-- :doc retrieve runs on a given date
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE rdate = :rdate

-- :name get-filtered-runs :? :*
-- :doc retrieve runs that match a set of filters
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE rdate >= :after-date and rdate <= :before-date
and distance >= :min-distance and distance <= :max-distance

-- :name get-ytd-mileage :? :*
-- :doc get the cumulative distance run in the current year
SELECT sum(r.distance*uc.factor) as distance
FROM runs r, unit_conversion uc
WHERE extract(year from r.rdate) = extract(year from now())
and uc.from_u = r.units
and uc.to_u = :units