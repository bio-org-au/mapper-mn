create schema mapper;

create sequence mapper.mapper_sequence start with 20;

create table mapper.db_version
(
    id      bigint  not null
        constraint db_version_pkey
            primary key,
    version integer not null
);

create table mapper.host
(
    id        bigint  default nextval('mapper.mapper_sequence'::regclass) not null
        constraint host_pkey
            primary key,
    host_name varchar(512)                                         not null,
    preferred boolean default false                                not null
);

create table mapper.match
(
    id         bigint  default nextval('mapper.mapper_sequence'::regclass) not null
        constraint match_pkey
            primary key,
    uri        varchar(255)                                         not null
        constraint uk_2u4bey0rox6ubtvqevg3wasp9
            unique,
    deprecated boolean default false                                not null,
    updated_at timestamp with time zone,
    updated_by varchar(255)
);

create table mapper.identifier
(
    id               bigint  default nextval('mapper.mapper_sequence'::regclass) not null
        constraint identifier_pkey
            primary key,
    id_number        bigint                                               not null,
    name_space       varchar(255)                                         not null,
    object_type      varchar(255)                                         not null,
    deleted          boolean default false                                not null,
    reason_deleted   varchar(255),
    updated_at       timestamp with time zone,
    updated_by       varchar(255),
    preferred_uri_id bigint
        constraint fk_k2o53uoslf9gwqrd80cu2al4s
            references mapper.match,
    version_number   bigint,
    constraint unique_name_space
        unique (version_number, id_number, object_type, name_space)
);

create index identifier_index
    on mapper.identifier (id_number, name_space, object_type);

create index identifier_version_index
    on mapper.identifier (id_number, name_space, object_type, version_number);

create index identifier_prefuri_index
    on mapper.identifier (preferred_uri_id);

create index identifier_id_version_object_index
    on mapper.identifier (id_number, object_type, version_number);

create index identifier_object_type_index
    on mapper.identifier (object_type);

create table mapper.identifier_identities
(
    match_id      bigint not null
        constraint fk_mf2dsc2dxvsa9mlximsct7uau
            references mapper.match,
    identifier_id bigint not null
        constraint fk_ojfilkcwskdvvbggwsnachry2
            references mapper.identifier,
    constraint identifier_identities_pkey
        primary key (identifier_id, match_id)
);

create index mapper_identifier_index
    on mapper.identifier_identities (identifier_id);

create index mapper_match_index
    on mapper.identifier_identities (match_id);

create index identity_uri_index
    on mapper.match (uri);

create table mapper.match_host
(
    match_hosts_id bigint
        constraint fk_iw1fva74t5r4ehvmoy87n37yr
            references mapper.match,
    host_id        bigint
        constraint fk_3unhnjvw9xhs9l3ney6tvnioq
            references mapper.host
);

create index match_host_index
    on mapper.match_host (match_hosts_id);

