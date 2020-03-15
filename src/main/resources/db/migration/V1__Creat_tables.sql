create table NEWS
(
    ID          BIGINT auto_increment,
    TITLE       TEXT,
    CONTENT     TEXT,
    URL         VARCHAR(1000),
    CREATED_AT  TIMESTAMP,
    MODIFIED_AT TIMESTAMP,
    primary key (ID)
);
create table LINKS_TO_BE_PROCESSED
(
    LINK VARCHAR(1000)
);

create table LINKS_ALREADY_PROCESSED
(
    LINK VARCHAR(1000)
)