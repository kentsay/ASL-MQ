/* Create table queue and relative file*/
CREATE TABLE IF NOT EXISTS queue
(
  qid SERIAL NOT NULL,
  qname VARCHAR(20) NOT NULL,
  qsize NUMERIC DEFAULT 0,
  create_date timestamp DEFAULT current_timestamp,
  update_date timestamp DEFAULT current_timestamp,
  PRIMARY KEY(qid),
  UNIQUE(qname)
);
CREATE INDEX qname ON queue (qname);

/* Create table msg and relative file*/
CREATE TABLE IF NOT EXISTS msg
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
CREATE TABLE IF NOT EXISTS msg_detail
(
  msg_id char(36) NOT NULL,
  msg_type msgtype_enum NOT NULL,
  msg_func msgfunc_enum NOT NULL,
  msg_detail varchar(2048),
  PRIMARY KEY(msg_id)
);
CREATE INDEX msg_type ON msg_detail (msg_type);

/* Function for insert data into msg table */
CREATE OR REPLACE FUNCTION func_insert_msg(m_id char(36), send_id varchar(100), recv_id varchar(100), q_id varchar(100)) RETURNS void AS
'
	INSERT INTO msg (mid, msend_id, mrecv_id, mqueue_id) VALUES (m_id,send_id,recv_id,q_id)
'
LANGUAGE sql;

/* Function for insert data into msg_detail table*/
CREATE OR REPLACE FUNCTION func_insert_msgdtl(m_id char(36), type varchar(20), functype varchar(20), msgdtl varchar(2048)) RETURNS void AS
'
	INSERT INTO msg_detail (msg_id, msg_type, msg_func, msg_detail) VALUES (m_id,CAST(type as msgtype_enum), CAST(functype as msgfunc_enum),msgdtl)
'
LANGUAGE sql;

/* Function for insert data into queue talbe*/
CREATE OR REPLACE FUNCTION func_insert_queue(q_name varchar(20)) RETURNS void AS
'
	INSERT INTO queue (qname) VALUES (q_name)
'
LANGUAGE sql;


/* Function for delete queue from queue table */
CREATE OR REPLACE FUNCTION func_delete_queue(q_name varchar(10)) RETURNS void AS 
'    
    DELETE FROM queue WHERE qname = q_name
' 
LANGUAGE SQL;


/* Function for delete msg from msg_detail table */
CREATE OR REPLACE FUNCTION func_delete_msg_detail(q_name varchar(10)) RETURNS void AS 
'
	DELETE FROM msg_detail WHERE msg_id in (SELECT mid FROM msg where mqueue_id = q_name)
'
LANGUAGE SQL;

/* Function for delete msg from msg table */
CREATE OR REPLACE FUNCTION func_delete_msg(q_name varchar(10)) RETURNS void AS 
'
	DELETE FROM msg WHERE mid in (SELECT mid FROM msg where mqueue_id = q_name)
'
LANGUAGE SQL;

/* Function for delete msg from msg_detail table */
CREATE OR REPLACE FUNCTION func_delete_msg_detail_byid(m_id char(36)) RETURNS void AS 
'
	DELETE FROM msg_detail WHERE msg_id=m_id
'
LANGUAGE SQL;

/* Function for delete msg from msg table */
CREATE OR REPLACE FUNCTION func_delete_msg_byid(m_id char(36)) RETURNS void AS 
'
	DELETE FROM msg WHERE mid=m_id
'
LANGUAGE SQL;

/* Function for update queue size */
CREATE OR REPLACE FUNCTION func_update_qsize (q_name varchar(10), size integer) RETURNS integer AS $$
    UPDATE queue
        SET qsize = (SELECT qsize from queue where qname=q_name) + size
        WHERE qname = q_name;
    SELECT 1;
$$ LANGUAGE SQL;