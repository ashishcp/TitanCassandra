# Titan + Cassandra Example

This sample shows how to write a Java application that pulls data out of a Titan graph and inserts data into a Cassandra table. This sample is designed to work with Titan 1.0.0 and Cassandra 2.1.x, 2.2.x.

This sample code was also run successfully with Titan 1.1.x and TinkerPop 3.1.x,
but that requires altering the pom.xml file dependencies from the defaults in
this package.

This sample code is not intended for use with massive graphs or volumes of data.

1) Download Titan 1.0.0.
  * You can download a prebuilt zip from [here](https://github.com/thinkaurelius/titan/wiki/Downloads).
   The download link for the prebuild zip is very confusing.
   Just ignore all of that text and download the
   [Titan 1.0.0 with Hadoop 1 – recommended](http://s3.thinkaurelius.com/downloads/titan/titan-1.0.0-hadoop1.zip)
   package.
  * You can download the source code from the [Titan Github repository](https://github.com/thinkaurelius/titan)
   using tag `1.0.0`. Build it with the directions in its [README](https://github.com/thinkaurelius/titan/blob/1.0.0/titan-dist/README.md).

2) Clone and build this sample package.  Navigate to the top level directory in this project
   where the `pom.xml` file is found and build the sample:

   ```
   $ cd ~/TitanCassandra
   $ mvn clean install
   ```

   After running the `mvn clean install` command to build the Sample, you should
   have the example built. There will be several new directories in the project root directory: `target`, `configure`, and `run`.

3) Copy the sample `titan-cassandra.properties` file from the configure directory in the Sample project into your Titan 1.0.0 installation's conf directory, and the sample `graph1.xml` GraphML file into its data directory:

   ```
   $ export TITAN_HOME=~/titan-1.0.0-hadoop1
   $ cp ./configure/titan-cassandra.properties $TITAN_HOME/conf/
   $ cp ./configure/graph1.xml $TITAN_HOME/data/
   ```

   Make sure that the properties in `titan-cassandra.properties` match your environment,
   particularly the `storage.hostname`. Save your changes.

   Navigate to your Titan 1.0.0 installation root directory, start the Gremlin Console,
   and create an empty Titan graph backed by Cassandra:

   ```
   $ cd $TITAN_HOME
   $ ./bin/gremlin.sh

            \,,,/
            (o o)
   -----oOOo-(3)-oOOo-----
   plugin activated: aurelius.titan
   plugin activated: tinkerpop.server
   plugin activated: tinkerpop.utilities
   plugin activated: tinkerpop.hadoop
   plugin activated: tinkerpop.tinkergraph
   gremlin> graph=TitanFactory.open('./conf/titan-cassandra.properties')
   ==>standardtitangraph[cassandrathrift:[10.10.10.100, 10.10.10.101, 10.10.10.102]]
   gremlin> graph.io(graphml()).readGraph('./data/graph1.xml')
   ==>null
   gremlin> graph.tx().commit()
   ==>null
   gremlin> g=graph.traversal()
   ==>graphtraversalsource[standardtitangraph[cassandrathrift:[10.10.10.100, 10.10.10.101, 10.10.10.102]], standard]
   gremlin> g.V().count()
   11:32:53 WARN  com.thinkaurelius.titan.graphdb.transaction.StandardTitanTx  - Query requires iterating over all vertices [()]. For better performance, use indexes
   ==>6
   gremlin> :q
   ```

4) Create an empty cassandra table where the Sample will write data.

   These next steps will use the `cqlsh` command in Cassandra.

   If you are running the Sample on a system that is remote from the Cassandra machine,
   locate the `createkeyspace.cql` file in the configure directory in the top level of the Sample
   project and copy it to the machine where Cassandra is running.
   The following `scp` command shows "pulling" the cql file onto the machine where `cqlsh`
   (Cassandra) is available.

   ```
   scp graphie@machine1.cloud.com:~/TitanCassandra/configure/createkeyspace.cql /tmp/
   ```

   Edit the `createkeyspace.cql` file that you copied and compare the datacenter and replication
   values to your Cassandra configuration.  These are the default values in the sample script: `'DC1': 3`.
   If you are running a single Cassandra instance, you may need to change the 3 (for replication)
   to 1, as an example. You can verify the datacenter with this command:

   ```
   $ ./bin/nodetool status
   Datacenter: DC1
   ===============
   Status=Up/Down
   |/ State=Normal/Leaving/Joining/Moving
   --  Address       Load       Tokens       Owns    Host ID                               Rack
   UN  10.10.10.100  211.05 KB  1            ?       8b01de29-e354-4c30-b787-50d2815c31d8  RAC1
   UN  10.10.10.101  217.61 KB  1            ?       6737c97c-d393-4329-8fcf-0e1c22cbe018  RAC1
   UN  10.10.10.102  256.46 KB  1            ?       7c2af7aa-a284-4177-be53-6d3a078116de  RAC1
   UN  10.10.10.103  247.77 KB  1            ?       57af34e7-029a-4434-b1c0-db7c45bcf568  RAC1

   Note: Non-system keyspaces don't have the same replication settings, effective ownership information is meaningless

   ```

   Use the `ifconfig` command in Linux to get the IP address of the machine where Cassandra is running,
   if you do not already know this. In this example, Cassandra is at `10.10.10.102`.

   ```
   $ ifconfig
        eth0      Link encap:Ethernet  HWaddr 07:A3:DB:41:47:AA
        inet addr:10.10.10.102  Bcast:10.10.10.225  Mask:255.255.255.0
   ```

   Enter the following command, which will create an empty keyspace and table in your Cassandra instance:

   ```
   $ export CASSANDRA_HOME=~/apache-cassandra-2.2.3
   $ cd $CASSANDRA_HOME
   $ ./bin/cqlsh 10.10.10.102 -f /tmp/createkeyspace.cql
   ```

   You can go into the cqlsh shell and verify the keyspace and table were created if you like:

   ```
   $ ./bin/cqlsh 10.10.10.102

   Connected to titancluster at 10.10.10.102:9042.
   [cqlsh 5.0.1 | Cassandra 2.2.3 | CQL spec 3.3.1 | Native protocol v4]
   Use HELP for help.
   cqlsh> describe keyspaces;

   system_auth  titandemo  demo  system_distributed
   system         system_traces

   cqlsh> use demo;
   cqlsh:demo> describe tables;

   alldata

   cqlsh:demo> quit
   ```

5) To recap, at this point you have:
   1. installed Titan 1.0.0
   2. built this Sample project
   3. created a Titan Graph backed by Cassandra and populated with a sample graph
   4. created an empty Cassandra table in a new keyspace
 
 
   Now we can run the Sample, showing how to pull data from the sample Titan graph and put it directly into Cassandra.

   Navigate to the top level directory of the Sample project, then navigate into the `run` subdirectory. You should see a properties file called `driver10.properties`. Update the values appropriately for your environment. You should see a script called `runit10.sh`. Update the `PROJPATH` appropriately for your environment, then run the script. The output should look something like this:

   ```
   $ cd ~/TitanCassandra/run
   $ ./runit10.sh

   Connecting To Cassandra Cluster
   SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
   12:09:14,920  INFO NettyUtil:83 - Did not find Netty's native epoll transport in the classpath, defaulting to NIO.
   12:09:15,527  INFO DCAwareRoundRobinPolicy:95 - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)
   12:09:15,531  INFO Cluster:1443 - New Cassandra host /10.10.10.100:9042 added
   12:09:15,531  INFO Cluster:1443 - New Cassandra host /10.10.10.101:9042 added
   12:09:15,531  INFO Cluster:1443 - New Cassandra host /10.10.10.102:9042 added
   12:09:15,531  INFO Cluster:1443 - New Cassandra host /10.10.10.103:9042 added
   Connected to cluster: titancluster
   Datatacenter: DC1; Host: /10.10.10.100; Rack: RAC1
   Datatacenter: DC1; Host: /10.10.10.101; Rack: RAC1
   Datatacenter: DC1; Host: /10.10.10.102; Rack: RAC1
   Datatacenter: DC1; Host: /10.10.10.103; Rack: RAC1
   Connecting To Cassandra Keyspace
   Now connect to a keyspace
   Connecting To Titan Graph
   12:09:16,200  INFO CassandraThriftStoreManager:612 - Closed Thrift connection pooler.
   12:09:16,208  INFO GraphDatabaseConfiguration:1518 - Generated unique-instance-id=0a5b47eb9009-kg2-48-sl-cloud9-ibm-com1
   12:09:16,235  INFO Backend:176 - Initiated backend operations thread pool of size 16
   12:09:16,357  INFO KCVSLog:730 - Loaded unidentified ReadMarker start time 2016-01-13T18:09:16.340Z into com.thinkaurelius.titan.diskstorage.log.kcvs.KCVSLog$MessagePuller@54dcfa5a
   12:09:16,447  WARN StandardTitanTx:1262 - Query requires iterating over all vertices [()]. For better performance, use indexes
   Inserting into Cassandra: marko,29
   Inserting into Cassandra: josh,32
   Inserting into Cassandra: peter,35
   Inserting into Cassandra: vadas,27
   Row: marko , 29
   Row: peter , 35
   Row: vadas , 27
   Row: josh , 32
   Disconnecting From Titan Graph
   12:09:16,613  INFO CassandraThriftStoreManager:612 - Closed Thrift connection pooler.
   Disconnecting From Cassandra Keyspace and Cluster
    ```

   Note, in some cases, if you change the classpath of the Sample program(s), you might see DEBUG statements like the following.
   Things like this particular message are harmless.

   ```
   15:28:01.459 [main] DEBUG c.t.t.g.database.StandardTitanGraph - Installed shutdown hook Thread[Thread-3,5,main]
   java.lang.Throwable: Hook creation trace
      at com.thinkaurelius.titan.graphdb.database.StandardTitanGraph.<init>(StandardTitanGraph.java:156) [titan-core-1.0.0.jar:na]
      at com.thinkaurelius.titan.core.TitanFactory.open(TitanFactory.java:94) [titan-core-1.0.0.jar:na]
      at com.thinkaurelius.titan.core.TitanFactory.open(TitanFactory.java:84) [titan-core-1.0.0.jar:na]
      at com.thinkaurelius.titan.core.TitanFactory$Builder.open(TitanFactory.java:139) [titan-core-1.0.0.jar:na]
      at com.example.Driver10.connectTitan(Driver10.java:250) [TitanLearning-1.0-SNAPSHOT.jar:na]
      at com.example.Driver10.doWork(Driver10.java:80) [TitanLearning-1.0-SNAPSHOT.jar:na]
      at com.example.Driver10.main(Driver10.java:46) [TitanLearning-1.0-SNAPSHOT.jar:na]
   ```

   Congratulations.  You can directly check the `demo.alldata` table in Cassandra for data using `cqlsh` if you like.


6) Realize that this Sample only scratches the surface around details about using Java to interface with Cassandra
   or even using Java to interact with a Titan graph.
