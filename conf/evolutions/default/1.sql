# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "users" ("id" SERIAL NOT NULL PRIMARY KEY,"username" VARCHAR(254) NOT NULL,"created_date" DATE NOT NULL);

# --- !Downs

drop table "users";

