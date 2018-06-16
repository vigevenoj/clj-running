-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-run! :! :n
-- :doc create a new run
INSERT INTO runs
(rdate, timeofday, distance, units, elapsed, effort, comment, shoeid)
VALUES (:rdate, :timeofday, :distance, :units, :elapsed, :effort, :comment :shoeid)

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
WHERE rdate >= current_date - interval ':limit days'

-- :name get-runs-by-date :? :*
-- :doc retrieve runs on a given date
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE rdate = :rdate

-- :name get-filtered-runs :? :*
-- :doc retrieve runs that match a set of filters
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid FROM runs
WHERE rdate >= :after-date and rdate <= :before-date
and distance >= :min-distance and distance <= :max-distance