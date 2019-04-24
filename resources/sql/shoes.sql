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