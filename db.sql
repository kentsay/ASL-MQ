/* Create user with password, and create db */
CREATE USER asl WITH PASSWORD 'asl';
CREATE DATABASE aslmq_db OWNER asl;

/* psql -d aslmq_db -U asl*/

/* Create table queue and relative file*/
CREATE TYPE qstatus_enum AS ENUM ('exists', 'delete');

CREATE TABLE queue
(
  qid SERIAL NOT NULL,
  qname VARCHAR(20) NOT NULL,
  qsize NUMERIC DEFAULT 0,
  qstatus qstatus_enum DEFAULT 'exists',
  create_date timestamp DEFAULT current_timestamp,
  update_date timestamp DEFAULT current_timestamp,
  PRIMARY KEY(qid),
  UNIQUE(qname)
);

CREATE INDEX qstatus ON queue (qstatus);

/* Create table client and relative file*/
CREATE TABLE client
(
  cid SERIAL NOT NULL,
  cip inet NOT NULL,
  create_date timestamp DEFAULT current_timestamp,
  PRIMARY KEY(cid)
);


/* Create table msg and relative file*/
CREATE TABLE msg
(
  mid char(36) NOT NULL,
  msend_id varchar(100) NOT NULL,
  mrecv_id varchar(100) NOT NULL,
  mqueue_id varchar(100) NOT NULL,
  marr_time timestamp DEFAULT current_timestamp,
  PRIMARY KEY(mid)
);

CREATE INDEX msend_id ON msg (msend_id);
CREATE INDEX mrecv_id ON msg (mrecv_id);
CREATE INDEX mqueue_id ON msg (mqueue_id);

/* Create table msg_detail and relative file*/
CREATE TYPE msgtype_enum AS ENUM ('send', 'receive', 'query');
CREATE TYPE msgfunc_enum AS ENUM ('ctr_createQ','ctr_deleteQ','send_toUser','send_toAll','read_byRmvMsg','read_byLookMsg','query_bySender','query_byQueue');
CREATE TYPE status_enum AS ENUM ('remove', 'exists');

CREATE TABLE msg_detail
(
  msg_id char(36) NOT NULL,
  msg_type msgtype_enum NOT NULL,
  msg_func msgfunc_enum NOT NULL,
  msg_detail varchar(2048),
  status status_enum,
  PRIMARY KEY(msg_id)
);

CREATE INDEX msg_type ON msg_detail (msg_type);
CREATE INDEX msg_status ON msg_detail (status);

