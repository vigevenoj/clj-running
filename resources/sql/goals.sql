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

-- :name get-goal-progress :? :1
-- :doc get current goal progress
select sum(r.distance * uc.factor) as distance,
    g.distance as goal_distance,
    sum(r.distance * uc.factor) / g.distance * 100 as percent_to_goal,
    g.end_date - g.start_date as goal_period_length,
    cast(current_date - g.start_date as decimal) / (g.end_date - g.start_date) * 100 as goal_period_elapsed_percentage
from runs r, unit_conversion uc, goals g
where uc.from_u = r.units
    and uc.to_u = g.distance_units -- i think this is right?
    and g.goalid = :goalid
    and r.distance is not null
    and rdate >= g.start_date and rdate <= g.end_date;

-- :name get-mileage-goal-progress-history :? :*
-- :doc get daily history of progress towards a mileage goal, in miles
select date_trunc('day', dd)::date as the_date,
    miles, sum(miles) over (order by dd) as total,
    (sum(miles) over (order by dd)) / :goaltotaldistance * 100 as percent
from generate_series(:goalstartdate, :goalenddate, '1 day') dd
left join daily_run_mileage on dd = daily_run_mileage.run_date
order by dd;