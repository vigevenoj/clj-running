CREATE TABLE unit_conversion (
  from_u varchar(5) default NULL,
  to_u varchar(5) default NULL,
  factor double precision default NULL
) ;
--;;
INSERT INTO unit_conversion VALUES ('m','km',0.001);
--;;
INSERT INTO unit_conversion VALUES ('km','m',1000);
--;;
INSERT INTO unit_conversion VALUES ('miles','km',1.609344);
--;;
INSERT INTO unit_conversion VALUES ('miles','m',1609.344);
--;;
INSERT INTO unit_conversion VALUES ('km','miles',0.621371192);
--;;
INSERT INTO unit_conversion VALUES ('m','miles',0.000621371192);
--;;
INSERT INTO unit_conversion VALUES ('miles','miles',1);
--;;
INSERT INTO unit_conversion VALUES ('km','km',1);
--;;
INSERT INTO unit_conversion VALUES ('m','m',1);
--;;
CREATE SEQUENCE public.runs_runid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--;;
CREATE TABLE public.runs (
    runid bigint default nextval('runs_runid_seq') not null,
    rdate date,
    timeofday character varying(4) DEFAULT NULL::character varying,
    distance numeric(6,2) DEFAULT NULL::numeric,
    units character varying(5) DEFAULT NULL::character varying,
    elapsed interval hour to second,
    effort character varying(50) DEFAULT NULL::character varying,
    comment character varying(200) DEFAULT NULL::character varying,
    shoeid integer
);