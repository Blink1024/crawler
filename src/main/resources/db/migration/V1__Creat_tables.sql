create table NEWS
(
    ID          BIGINT primary key auto_increment,
    TITLE       TEXT,
    CONTENT     TEXT,
    URL         VARCHAR(1000),
    CREATED_AT  TIMESTAMP default now(),
    MODIFIED_AT TIMESTAMP default now()
) DEFAULT CHARSET = utf8mb4;
create table LINKS_TO_BE_PROCESSED
(
    LINK VARCHAR(1000)
);

create table LINKS_ALREADY_PROCESSED
(
    LINK VARCHAR(1000)
)