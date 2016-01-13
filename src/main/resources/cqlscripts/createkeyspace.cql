-- copy this to a node where cassandra is running
-- and use cqlsh to create the test keyspace and table.
--
-- run this using the same user used to run cassandra.
-- $CASSANDRA_HOME/bin/cqlsh <ip of cassandra machine> -f /path/to/createkeyspace.cql
-- or from within cqlsh shell: SOURCE '/path/to/createkeyspace.cql'

-- change DC1 to match data center settings for your cassandra cluster
-- often found in cassandra-rackdc.properties
-- note replication factor of 3 is currently set - assuming 3 nodes.
-- you can set this as low as 1 for testing this out.
--

CREATE KEYSPACE demo
   WITH REPLICATION = { 'class': 'NetworkTopologyStrategy', 'DC1': 3};

-- cassandra column family (table) that stores our data
-- referenced by <keyspace.tablename>
CREATE TABLE demo.alldata (
  name      text,
  age       int,
  PRIMARY KEY (name)
);
