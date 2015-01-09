/* Create table queue and relative file*/
CREATE TABLE IF NOT EXISTS queue
(
  qname integer NOT NULL,
  qsize NUMERIC DEFAULT 0,
  create_date timestamp DEFAULT current_timestamp,
  update_date timestamp DEFAULT current_timestamp,
  PRIMARY KEY(qname)
);

/* Create table msg and relative file*/
CREATE TYPE msgtype_enum AS ENUM ('send', 'receive', 'query');
CREATE TYPE msgfunc_enum AS ENUM ('ctr_createQ','ctr_deleteQ','send_toUser','send_toAll','read_byRmvMsg','read_byLookMsg','query_bySender','query_byQueue');

CREATE TABLE IF NOT EXISTS msg
(
  mid SERIAL NOT NULL,
  msend_id integer NOT NULL,
  mrecv_id integer NOT NULL,
  mqueue_id integer NOT NULL,
  msg_type msgtype_enum NOT NULL,
  msg_func msgfunc_enum NOT NULL,
  msg_detail varchar(2048),
  marr_time timestamp DEFAULT current_timestamp
);
CREATE INDEX idx_msend_mrecv_mqueue ON msg (msend_id, mrecv_id, mqueue_id);

/* Function for insert data into msg table */
CREATE OR REPLACE FUNCTION func_insert_msg(send_id varchar(20), recv_id varchar(20), q_id varchar(20), type varchar(20), functype varchar(20), msgdtl varchar(2048)) RETURNS void AS
'
	INSERT INTO msg (msend_id, mrecv_id, mqueue_id, msg_type, msg_func, msg_detail) 
  VALUES (CAST(send_id as integer),CAST(recv_id as integer),CAST(q_id as integer),CAST(type as msgtype_enum), CAST(functype as msgfunc_enum),msgdtl)
'
LANGUAGE sql;

/* Function for insert data into msg_detail table*/
/*CREATE OR REPLACE FUNCTION func_insert_msgdtl(m_id char(36), type varchar(20), functype varchar(20), msgdtl varchar(2048)) RETURNS void AS
'
	INSERT INTO msg_detail (msg_id, msg_type, msg_func, msg_detail) VALUES (m_id,CAST(type as msgtype_enum), CAST(functype as msgfunc_enum),msgdtl)
'
LANGUAGE sql;
*/
/* Function for insert data into queue talbe*/
CREATE OR REPLACE FUNCTION func_insert_queue(q_name varchar(20)) RETURNS void AS
'
	INSERT INTO queue (qname) VALUES (CAST(q_name as integer))
'
LANGUAGE sql;


/* Function for delete queue from queue table */
CREATE OR REPLACE FUNCTION func_delete_queue(q_name varchar(20)) RETURNS void AS 
'    
    DELETE FROM queue WHERE qname = CAST(q_name as integer)
' 
LANGUAGE SQL;


/* Function for delete msg from msg_detail table */
/*
CREATE OR REPLACE FUNCTION func_delete_msg_detail(q_name varchar(10)) RETURNS void AS 
'
	DELETE FROM msg_detail WHERE msg_id in (SELECT mid FROM msg where mqueue_id = q_name)
'
LANGUAGE SQL;
*/

/* Function for delete msg from msg table */
CREATE OR REPLACE FUNCTION func_delete_msg(q_name varchar(20)) RETURNS void AS 
'
	DELETE FROM msg WHERE mid in (SELECT mid FROM msg where mqueue_id = CAST(q_name as integer))
'
LANGUAGE SQL;

/* Function for delete msg from msg_detail table */
/*
CREATE OR REPLACE FUNCTION func_delete_msg_detail_byid(m_id char(36)) RETURNS void AS 
'
	DELETE FROM msg_detail WHERE msg_id=m_id
'
LANGUAGE SQL;
*/

/* Function for delete msg from msg table */
CREATE OR REPLACE FUNCTION func_delete_msg_byid(m_id integer) RETURNS void AS 
'
	DELETE FROM msg WHERE mid=m_id
'
LANGUAGE SQL;

/* Function for update queue size */
/*
CREATE OR REPLACE FUNCTION func_update_qsize (q_name varchar(10), size integer) RETURNS integer AS $$
    UPDATE queue
        SET qsize = (SELECT qsize from queue where qname=q_name) + size
        WHERE qname = q_name;
    SELECT 1;
$$ LANGUAGE SQL;
*/