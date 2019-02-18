create sequence if not exists goals_goalid_seq
    start with 1
    increment by 1
    minvalue 1
    no maxvalue
    cache 1;
--;;
create table if not exists goals(
    goalid bigint default nextval('goals_goalid_seq') not null,
    start_date date,
    end_date date,
    is_met boolean,
    type character varying(50),
    distance numeric(6,2),
    distance_units character varying(5),
    pace interval hour to second
);
