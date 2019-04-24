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
select dd::date as rdate, coalesce(sum(miles), 0) as distance
from generate_series('2003-01-01', current_date, '1 day') dd
left join daily_run_mileage on dd = daily_run_mileage.run_date
group by rdate order by rdate;

-- :name daily-miles-by-year :? :*
-- :doc get sum of miles run per day for a given year
-- this query uses the daily_run_mileage view
-- this query returns a row for every date between Jan 1 and Dec 31 (inclusive) of the given year
select dd::date as rdate, coalesce(sum(miles), 0) as distance
from generate_series(make_date(:year, 1, 1), make_date(:year, 12, 31), '1 day') dd
left join daily_run_mileage on dd = daily_run_mileage.run_date
group by rdate order by rdate;

-- :snip union-year
-- :doc snippet to get a year's worth of dates
union select generate_series('2019-01-01'::timestamp, date_trunc('year', '2019-01-01'::timestamp) + '1 year'::interval - '1 day'::interval , '1 day') dd

-- :name silly-by-year :? :*
-- :doc hacking around
select dd:date as rdate, coalesce(sum(miles), 0) as distance
from
(select null as dd
--~ (map (map #(hugsql.core/sqlvec "union select generate_series(:date, date+trunc(:date) + '1 year'::interval - '1 day'::interval, '1 day')" {:year (java-time/local-date %)}) (:years params))
)
left join daily_run_mileage on dd = daily_run_mileage.run_date
where dd is not null
group by rdate order by rdate;

-- :snip year-series
-- :doc generate a series


-- :name get-daily-distance-by-years :? :*
-- :doc get total daily distance per day for one or more years
-- this is for the heatmap data
select r.rdate, coalesce(sum(r.distance * uc.factor), 0) as distance
from runs r, unit_conversion uc
where uc.from_u = r.units and uc.to_u = :units
and extract(year from r.rdate) in (:v*:years)
group by r.rdate
order by r.rdate asc

-- :name get-daily-distance-all-years :? :*
-- :doc get total daily distance per day for one or more years
-- this is for the heatmap data
select r.rdate, coalesce(sum(r.distance * uc.factor), 0) as distance
from runs r, unit_conversion uc
where uc.from_u = r.units and uc.to_u = :units
group by r.rdate
order by r.rdate asc

-- :name get-total-cumulative-distance :? :1
-- :doc get the cumulative total distance of all runs
SELECT sum(r.distance*uc.factor) as distance
FROM runs r, unit_conversion uc
WHERE uc.from_u = r.units AND uc.to_u = :units AND r.distance is not null