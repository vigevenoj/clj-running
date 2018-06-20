CREATE TABLE unit_conversion (
  from_u varchar(5) default NULL,
  to_u varchar(5) default NULL,
  factor double precision default NULL
) ;

CREATE SEQUENCE public.runs_runid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.runs (
    runid bigint,
    rdate date,
    timeofday character varying(4) DEFAULT NULL::character varying,
    distance numeric(6,2) DEFAULT NULL::numeric,
    units character varying(5) DEFAULT NULL::character varying,
    elapsed interval hour to second,
    effort character varying(50) DEFAULT NULL::character varying,
    comment character varying(200) DEFAULT NULL::character varying,
    shoeid integer
);