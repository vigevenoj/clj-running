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

-- :name create-run! :! :n
-- :doc create a new run
INSERT INTO runs
(rdate, timeofday, distance, units, elapsed, effort, comment, shoeid)
VALUES (:rdate, :timeofday, :distance, :units, :elapsed, :effort, :comment, :shoeid)
RETURNING runid

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

-- :name get-latest-runs :? :*
-- :doc get the latest runs regardless of how long ago they happened
SELECT runid, rdate, timeofday, distance, units, elapsed, effort, comment, shoeid
FROM runs
ORDER BY rdate desc
LIMIT :limit

-- :name get-current-year-distance :? :*
-- :doc get the cumulative distance run in the current calendar year
SELECT coalesce(sum(r.distance*uc.factor), 0) as distance
FROM runs r, unit_conversion uc
WHERE extract(year from r.rdate) = extract(year from now())
and uc.from_u = r.units
and uc.to_u = :units

-- :name get-current-month-distance :? :*
-- :doc get the cumulative distance in the current calendar month
select coalesce(sum(r.distance*uc.factor), 0) as distance
from runs r, unit_conversion uc
where extract(year from r.rdate) = extract(year from now())
and extract(month from r.rdate) = extract(month from now())
and uc.from_u = r.units and uc.to_u = :units;

-- :name get-current-week-distance :? :*
-- :doc get the cumulative distance in the current week (ISO week, starts on monday)
select coalesce(sum(r.distance*uc.factor), 0) as distance
from runs r, unit_conversion uc
where extract(year from r.rdate) = extract(year from now())
and extract(week from r.rdate) = extract(week from now())
and uc.from_u = r.units and uc.to_u = :units;

-- :name get-rolling-period-distance :? :*
-- :doc get the cumulative distance over the last period
select coalesce(sum(r.distance*uc.factor), 0) as distance
from runs r, unit_conversion uc
where rdate >= current_date - :period::interval
and uc.from_u = r.units and uc.to_u = :units


-- :name get-total-cumulative-distance :? :1
-- :doc get the cumulative total distance of all runs
SELECT sum(r.distance*uc.factor) as distance
FROM runs r, unit_conversion uc
WHERE uc.from_u = r.units AND uc.to_u = :units AND r.distance is not null

-- :name get-all-shoes :? :*
-- :doc get all shoes
select shoeid, name, description, cumulative_distance, cumulative_distance_units, distance_expiration, distance_expiration_units, is_active
from shoes;

-- :name get-shoe :? :1
-- :doc get a single shoe by shoeid
select shoeid, name, description, cumulative_distance, cumulative_distance_units, distance_expiration, distance_expiration_units, is_active
from shoes
where shoeid = :shoeid;

-- :name create-shoe! :! :n
-- :doc create a new shoe
INSERT INTO shoes
(name, description, cumulative_distance, cumulative_distance_units, distance_expiration, distance_expiration_units, is_active)
VALUES
(:name, :description, :cumulative_distance, :cumulative_distance_units, :distance_expiration, :distance_expiration_units, :is_active)
RETURNING shoeid;

-- :name update-shoe! :! :n
-- :doc update a shoe by ID
UPDATE shoes SET
name = : name, description = :description, cumulative_distance = :cumulative_distance, cumulative_distance_units = :cumulative_distance_units,
distance_expiration = :distance_expiration, distance_expiration_units = :distance_expiration_units, is_active = :is_active
WHERE shoeid = :shoeid;

-- :name delete-shoe! :! :n
-- :doc deletes a shoe given the id
DELETE FROM shoes
WHERE shoeid = :shoeid;


-- :name create-goal! ! :! :n
-- :doc create a new goal
INSERT INTO goals
(start_date, end_date, is_met, type, distance, distance_units, pace)
VALUES
(:start_date, :end_date, :is_met, :type, :distance, :distance_units, :pace)
RETURNING goalid;

-- :name get-all-goals :? :*
-- :doc get all goals
SELECT goalid, start_date, end_date, is_met, type, distance, distance_units, pace
FROM goals;

-- :name get-goal :? :1
-- :doc get a single goal by its ID
SELECT goalid, start_date, end_date, is_met, type, distance, distance_units, pace
FROM goals
WHERE goalid = :goalid;

-- :name update-goal! :! :n
-- :doc update an existing goal
UPDATE goals
SET start_date = :start_date, end_date = :end_date, is_met = :is_met, type = :type, distance = :distance,
distance_units = :distance_units, pace = :pace
WHERE goalid = :goalid;

-- :name delete-goal! :! :n
-- :doc delete a goal by its ID
DELETE FROM goals WHERE goalid = :goalid;