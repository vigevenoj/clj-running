create view daily_run_mileage as SELECT sum(r.distance * uc.factor) AS miles,
    r.rdate AS run_date
   FROM runs r,
    unit_conversion uc
  WHERE uc.from_u = r.units AND uc.to_u = 'miles'
  GROUP BY r.rdate;