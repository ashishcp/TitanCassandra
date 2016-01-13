package com.example;

// datastax core driver used here
// might also peruse the java driver at some point
import com.datastax.driver.core.*;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This demonstrates how to read data from a Titan graph and write
 * the resulting data to a Cassandra table in one program.
 *
 * Ideally this class would be broken into separate classes.
 *
 * But the purposes of this sample - for one stop shopping -
 * all of the code necessary to read from Titan and write
 * to Cassandra is in this one giant class.
 *
 * Note this uses the Datastax core driver, which implies certain
 * things about how this example interfaces to Cassandra.
 */
public class Driver10
{
    /**
     *
     * @param args
     */
    public static void main(String args[])
    {
        String propsFile = "bogus";
        if (args.length > 0)
        {
            propsFile = args[0];
            Driver10 me = new Driver10();
            me.doWork(propsFile);
        }
        else
        {
            usage();
        }
    }

    /**
     *
     */
    public static void usage()
    {
        System.out.println("Driver10 <path and name of props file>");
    }


    /**
     * Query a graph, get back results and push results
     * to a predefined Cassandra table - i.e., the keyspace
     * and table in Cassandra must already be defined before running
     * this program.
     */
    private void doWork(String propsFile)
    {
        PropsValues props = readPropsFile(propsFile);

        System.out.println("Connecting To Cassandra Cluster");
        connectCassandraCluster(props);

        System.out.println("Connecting To Cassandra Keyspace");
        connectCassandraSession(props);

        System.out.println("Connecting To Titan Graph");
        connectTitan(props);

        // read from Titan
        readFromTitanStoreIntoCassandra(props);

        // read data back from Cassandra for fun
        readDataFromCassandra(props);

        // close stuff and leave
        System.out.println("Disconnecting From Titan Graph");
        closeTitan(props);

        System.out.println("Disconnecting From Cassandra Keyspace and Cluster");
        closeCassandraAll(props);
    }


    /**
     * Execute a Gremlin Query passed into this program and store
     * the results into Cassandra.  While this pretends to be "configurable"
     * to show the idea, in reality, the query passed in has to match the
     * cassandra storing logic at the moment...so almost like being hard-coded.
     * @param props
     */
    private void readFromTitanStoreIntoCassandra(PropsValues props)
    {
        // traversal is different from structure in TP 3.x
        GraphTraversalSource g = props.graph.traversal();

        // The Gremlin Query you want to execute
        ArrayList allVerts = new ArrayList();

        /*** HERE IS ONE GREMLIN QUERY...IT GETS ALL OF THE VERTICES ***/
        /*** A QUERY LIKE THIS IS A BAD IDEA ON A LARGE GRAPH ***/
        g.V().fill(allVerts);

        for (Object o: allVerts)
        {
            Vertex v = (Vertex)o;

            ArrayList m = new ArrayList();
            g.V(v).properties().fill(m);

            String thisName  = "bogus";
            int     thisAge  = -1;
            int     set      = 0;
            for (Object vp: m)
            {

                VertexProperty aVp = (VertexProperty)vp;
                if (aVp.key().equalsIgnoreCase("name"))
                {
                    thisName = (String)aVp.value();
                    set++;
                }
                if (aVp.key().equalsIgnoreCase("age"))
                {
                    // code hopes this is an integer value....
                    thisAge = Integer.parseInt(aVp.value().toString());
                    set++;
                }
            }
            if (set > 1)
                persistDataToCassandra(props, thisName, thisAge);
        }
    }


    /**
     * Read properties from a properties file.
     * Save properties in an internal class.
     */
    private PropsValues readPropsFile(String propsFile)
    {
        Properties props = new Properties();
        Reader reader = null;
        PropsValues propsValues = null;

        try
        {
            propsValues = new PropsValues();
            reader = new FileReader(propsFile);
            props.load(reader);

            // Cassandra Properties
            propsValues.cIpAddress = props.getProperty("cassip");

            String tmpPort         = props.getProperty("cassport");
            if (tmpPort == null)  tmpPort = "9042";
            propsValues.cCassPort  = Integer.parseInt(tmpPort);

            // Titan Properties
            propsValues.storagecassandrakeyspace = props.getProperty("storage.cassandra.keyspace");
            propsValues.storagebackend           = props.getProperty("storage.backend");
            propsValues.storagehostname          = props.getProperty("storage.hostname");
            // bonus...but like to set this now
            propsValues.gremlingraph             = props.getProperty("gremlin.graph");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            }
            catch (Exception e)
            {
                // don't care
                e.printStackTrace();
            }
        }
        return propsValues;
    }


    /**
     *
     */
    public void connectCassandraCluster(PropsValues props)
    {
        // connect to cassandra cluster given IP and Port.  Look in cassandra.yaml for values for port
        props.cluster = Cluster.builder().withPort(props.cCassPort).addContactPoint(props.cIpAddress).build();

        Metadata metadata = props.cluster.getMetadata();

        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for ( Host host : metadata.getAllHosts() )
        {
            System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",  host.getDatacenter(), host.getAddress(), host.getRack());
        }
    }


    /**
     *
     * @param props
     */
    public void connectCassandraSession(PropsValues props)
    {

        // https://academy.datastax.com/demos/getting-started-apache-cassandra-and-java-part-i
        System.out.println("Now connect to a keyspace");
        try
        {
            props.session = props.cluster.connect("demo");
        }
        catch (com.datastax.driver.core.exceptions.InvalidQueryException iqe)
        {
            System.out.println(iqe.getMessage());
        }
    }

    /**
     *  Note that if the graph doesn't already exist, Titan's behavior is to
     *  create an empty graph.  Since this demo is assuming there is some
     *  existing graph to connect to, it makes no effort to define schema
     *  or any other such thing.
     */
    private void connectTitan(PropsValues props)
    {
        try
        {
            // wonderful syntax, if you are into this sort of thing....
            // can do this equally effectively setting up a Configuration and passing that in
            props.graph = TitanFactory.build().set("storage.cassandra.keyspace", props.storagecassandrakeyspace).
                    set("storage.backend", props.storagebackend).set("storage.hostname", props.storagehostname).
                    set("gremlin.graph", props.gremlingraph).open();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    /**
     *  Closes session and cluster connections
     */
    private void closeCassandraAll(PropsValues props)
    {
        // TODO: check for nulls and exceptions
        try
        {
            if (props.session != null)
            {
                props.session.close();
                props.session = null;
            }

            if (props.cluster != null)
            {
                props.cluster.close();
                props.cluster = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param props
     */
    private void closeTitan(PropsValues props)
    {
        try
        {
            if (props.graph != null)
                props.graph.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    /**
     *
     * @param props
     */
    private void persistDataToCassandra(PropsValues props, String name, int age)
    {
        System.out.println("Inserting into Cassandra: " + name + "," + age);
        props.session.execute(
                "INSERT INTO demo.alldata (name, age) VALUES (?, ?)",
                 name, age);
    }


    /**
     *
     */
    public void readDataFromCassandra(PropsValues props)
    {
        final ResultSet fetchResults = props.session.execute(
                                       "SELECT name,age from demo.alldata limit 100");

        // Java 5+ method of iterating
        for (Row allDataRow : fetchResults)
        {
            String nameFetched = allDataRow.getString("name");
            int ageFetched = allDataRow.getInt("age");
            System.out.println("Row: " + nameFetched + " , " + ageFetched);
        }
    }



    /*
     * contains the properties used to connect to titan and cassandra
     */
    public class PropsValues
    {
        /* A handle to the cassandra cluster */
        public Cluster cluster;

        /* A handle to a particular keyspace in the cassandra cluster */
        public Session session;

        public String cIpAddress;
        public int    cCassPort;

        /* A handle to the Titan graph */
        TitanGraph graph;

        public String storagecassandrakeyspace;
        public String storagebackend;
        public String storagehostname;
        public String gremlingraph;
    }
}
