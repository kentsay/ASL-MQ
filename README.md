Advanced Systems Lab Project
======

The goal of this project is to develop, performance evaluation, and modeling of a message passing middleware platform supporting persistent queues and a simple message format. The idea is to build a simpler version of systems such as Apache ActiveMQ, IBM MQ Series, or JBoss Messaging. We used Java as the programming language and PostgreSQL as the database. 

There are three layers in this system: client, message middleware and the database. The basic functionality the system support is as follows:

1. Messaging
  * Clients can create and delete queues
  * Clients can send and receive messages
  * Clients can read a queue by either removing the topmost message or just by looking at its contents
  * Clients can send a message to a queue indicating a particular receiver
  * If a message has an explicit receiver, it can only be accessed by that receiver (but there is no need to implement a client authentication system)
  * Clients can query for messages from a particular sender (at most one message is returned)
  * Clients can query for queues where messages for them are waiting
2. Management
  * Clients use fixed accounts in the messaging system and are uniquely identified by these accounts
  * All clients can send and receive from any queue (no access control in the system)
  * Sending or reading from non-existing queues, reading from an empty queue, or failing to create or delete new queues are events that must raise well defined errors
3. Persistence
  * Clients, queues, and messages are stored persistently in a database (lowest tier). This information must survive system failures
  * Queues and messages are indexed

How to run the system 
======

The system is compile and run by using ANT. Before running this, you should have your PostgreSQL database and schema ready. Script for setting up database can refer to `sql/asldb_v2.sql`.

After you have your database running, config the machine setting under `config/dbservice.properties`. You can have several db farm setting in this config. The production farm can be set in build.xml.

First, start the middleware server by running `ant run`. Then you can execute one client starting to send message by `ant send`. All of the message content is manage under `data` folder. Average throughput of the message system is around 900 request per second.
