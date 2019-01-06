create sequence if not exists public.shoes_shoeid_seq
    start with 1
    increment by 1
    minvalue 1
    no maxvalue
    cache 1;
--;;
create table if not exists shoes (
    shoeid bigint default nextval('shoes_shoeid_seq') not null,
    name character varying(50),
    description character varying(200),
    cumulative_distance numeric(6,2),
    cumulative_distance_units character varying(5),
    distance_expiration numeric(6,2),
    distance_expiration_units character varying(5),
    is_active boolean
);