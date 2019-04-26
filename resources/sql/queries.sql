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

-- :name get-distance-with-pace :? :*
-- :doc get distance and pace information about runs
select r.rdate, r.distance * uc.factor as distance,
r.elapsed, uc.to_u as units,
extract(epoch from elapsed) / (r.distance * uc.factor) * '1 second'::interval as pace
from runs r, unit_conversion uc
where uc.from_u = r.units and uc.to_u = :units


-- :name daily-miles-all-years :? :*
-- :doc get sum of miles run per day for all years
-- this query uses the daily_run_mileage view
-- this query returns a row for every date between 2003-01-01 and today
-- the get-daily-distance-all-years query goes from the earliest run until latest run
select dd::date as rdate, coalesce(sum(miles), 0) as distance
from generate_series('2003-01-01', current_date, '1 day') dd
left join daily_run_mileage on dd = daily_run_mileage.run_date
group by rdate order by rdate;

-- :name get-daily-distance-all-years :? :*
-- :doc get total daily distance per day for one or more years
-- this is for the heatmap data
-- this query uses the earliest and latest runs as the start/end of the sequence to join against
select rd.rdate::date as rdate, coalesce(sum(distance), 0) as distance
from (select generate_series(min(runs.rdate), max(runs.rdate), interval '1 day') as rdate from runs) as rd
left join
(select r.rdate as rdate, r.distance * uc.factor as distance from runs r, unit_conversion uc where uc.from_u = r.units and uc.to_u = :units) as runs on rd.rdate = runs.rdate group by rd.rdate order by rd.rdate;

-- :name get-total-cumulative-distance :? :1
-- :doc get the cumulative total distance of all runs
SELECT sum(r.distance*uc.factor) as distance
FROM runs r, unit_conversion uc
WHERE uc.from_u = r.units AND uc.to_u = :units AND r.distance is not null