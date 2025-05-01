create table if not exists m_test (

    embedding vector(1536,FLOAT64)

);

create table if not exists dog
(
    id          integer primary key,
    name        varchar(255) not null,
    owner       varchar(255) null,
    description varchar(255) not null
);

