/* Create user with password, and create db */
CREATE USER asl WITH PASSWORD 'asl';
CREATE DATABASE aslmq_db OWNER asl;

/* psql -d aslmq_db -U asl*/