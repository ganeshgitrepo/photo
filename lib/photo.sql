-- Copyright (c) 1998  Dustin Sallings
--
-- $Id: photo.sql,v 1.1 1998/04/12 03:14:18 dustin Exp $
--
-- Use this to bootstrap your SQL database to do cool shite with the
-- photo album.

-- Where the picture info is stored.

create table album(
    fn       varchar(32),
    keywords varchar(50),
    descr    text,
    ts       timestamp,
    cat      int,
    taken    date,
    size     int
);

create unique index album_byoid on album(oid);
grant all on album to nobody;

-- The categories

create table cat(
    id       int,
    name     varchar
);

grant all on cat to nobody;

-- A sequence for generating categories

create sequence cat_seq;
grant all on cat_seq to nobody;

-- The ACLs for the categories

create table wwwacl(
    username varchar(16),
    cat      int
);

create index acl_byname on wwwacl(username);
grant all on wwwacl to nobody;

-- The group file for the Web server's ACL crap.

create table wwwgroup(
    username varchar(16),
    groupname varchar(16)
);

grant all on wwwgroup to nobody;

-- The passwd file for the Web server's ACL crap.

create table wwwusers(
    username varchar(16),
    password char(13),
    email    varchar,
    realname varchar
);

create index user_byname on wwwusers(username);
grant all on wwwusers to nobody;