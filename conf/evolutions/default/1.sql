# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "ftp_credentials" ("id" SERIAL NOT NULL PRIMARY KEY,"ftp_site" VARCHAR(254) NOT NULL,"username" VARCHAR(254) NOT NULL,"password" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "local_dir" ("id" SERIAL NOT NULL PRIMARY KEY,"path" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "pref_header" ("id" SERIAL NOT NULL PRIMARY KEY,"user_id" INTEGER NOT NULL,"name" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "release" ("id" SERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"user_id" INTEGER NOT NULL,"study_name" VARCHAR(254) NOT NULL,"study_abstract" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "release_tsv_file_link" ("id" SERIAL NOT NULL PRIMARY KEY,"release_id" INTEGER NOT NULL,"tsv_file_id" INTEGER NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "sample" ("id" SERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"complete" BOOLEAN NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "sample_file" ("id" SERIAL NOT NULL PRIMARY KEY,"filename" VARCHAR(254) NOT NULL,"path" VARCHAR(254) NOT NULL,"origin" VARCHAR(254) NOT NULL,"swid" INTEGER NOT NULL,"sample" VARCHAR(254) NOT NULL,"library" VARCHAR(254) NOT NULL,"sequencer_run" VARCHAR(254) NOT NULL,"lane" INTEGER NOT NULL,"barcode" VARCHAR(254) NOT NULL,"read" INTEGER NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "sample_sample_file_link" ("id" SERIAL NOT NULL PRIMARY KEY,"sample_id" INTEGER NOT NULL,"sample_file_id" INTEGER NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "tsv_file" ("id" SERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"path" VARCHAR(254) NOT NULL,"sample_file_type" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "tsv_file_sample_link" ("id" SERIAL NOT NULL PRIMARY KEY,"tsv_file_id" INTEGER NOT NULL,"sample_id" INTEGER NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
create table "users" ("id" SERIAL NOT NULL PRIMARY KEY,"username" VARCHAR(254) NOT NULL,"created_tstmp" TIMESTAMP NOT NULL);
alter table "pref_header" add constraint "USER_FK" foreign key("user_id") references "users"("id") on update NO ACTION on delete NO ACTION;
alter table "release" add constraint "user_FK" foreign key("user_id") references "users"("id") on update NO ACTION on delete NO ACTION;
alter table "release_tsv_file_link" add constraint "TSV_FILE_FK" foreign key("tsv_file_id") references "tsv_file"("id") on update NO ACTION on delete NO ACTION;
alter table "release_tsv_file_link" add constraint "RELEASE_FK" foreign key("release_id") references "release"("id") on update NO ACTION on delete NO ACTION;
alter table "sample_sample_file_link" add constraint "SAMPLE_FILE_FK" foreign key("sample_file_id") references "sample_file"("id") on update NO ACTION on delete NO ACTION;
alter table "sample_sample_file_link" add constraint "SAMPLE_FK" foreign key("sample_id") references "sample"("id") on update NO ACTION on delete NO ACTION;
alter table "tsv_file_sample_link" add constraint "TSV_FILE_FK" foreign key("tsv_file_id") references "tsv_file"("id") on update NO ACTION on delete NO ACTION;
alter table "tsv_file_sample_link" add constraint "SAMPLE_FK" foreign key("sample_id") references "sample"("id") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "pref_header" drop constraint "USER_FK";
alter table "release" drop constraint "user_FK";
alter table "release_tsv_file_link" drop constraint "TSV_FILE_FK";
alter table "release_tsv_file_link" drop constraint "RELEASE_FK";
alter table "sample_sample_file_link" drop constraint "SAMPLE_FILE_FK";
alter table "sample_sample_file_link" drop constraint "SAMPLE_FK";
alter table "tsv_file_sample_link" drop constraint "TSV_FILE_FK";
alter table "tsv_file_sample_link" drop constraint "SAMPLE_FK";
drop table "ftp_credentials";
drop table "local_dir";
drop table "pref_header";
drop table "release";
drop table "release_tsv_file_link";
drop table "sample";
drop table "sample_file";
drop table "sample_sample_file_link";
drop table "tsv_file";
drop table "tsv_file_sample_link";
drop table "users";

