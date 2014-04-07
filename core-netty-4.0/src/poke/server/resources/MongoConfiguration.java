package poke.server.resources;

import misc.LoggingOptions;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.io.InputStream;

/**
 * Created by poojasrinivas on 4/4/14.
 */
public class MongoConfiguration {
    protected static Logger logger = LoggerFactory.getLogger("MongoConfiguration");
    @JsonProperty
    private static String node_one;
    @JsonProperty
    private static String node_two;
    @JsonProperty
    private static String node_three;
    @JsonProperty
    private static String node_four;
    @JsonProperty
    private static String node_zero;
    @JsonProperty
    private static int port_zero;
    @JsonProperty
    private static int port_one;
    @JsonProperty
    private static int port_two;
    @JsonProperty
    private static int port_three;
    @JsonProperty
    private static int port_four;
    @JsonProperty
    private static String dbName;

    public MongoConfiguration() throws IOException{

        Properties properties = new Properties();
        InputStream input = null;

        String fileName = "src/properties.mongo";
        input = MongoConfiguration.class.getClassLoader().getResourceAsStream(fileName);

        if(input==null){
            logger.info("Sorry, unable to find " + fileName);
            return;
        }

        properties.load(input);

        dbName = properties.getProperty("dbName");
        node_zero = properties.getProperty("node_zero");
        node_one = properties.getProperty("node_one");
        node_two = properties.getProperty("node_two");
        node_three = properties.getProperty("node_three");
        node_four = properties.getProperty("node_four");
        port_zero = Integer.parseInt(properties.getProperty("port_zero"));
        port_one = Integer.parseInt(properties.getProperty("port_one"));
        port_two = Integer.parseInt(properties.getProperty("port_two"));
        port_three = Integer.parseInt(properties.getProperty("port_three"));
        port_four = Integer.parseInt(properties.getProperty("port_four"));

    }

    public static String getNode_one() {
        return node_one;
    }

    public static void setNode_one(String node_one) {
        MongoConfiguration.node_one = node_one;
    }

    public static String getNode_two() {
        return node_two;
    }

    public static void setNode_two(String node_two) {
        MongoConfiguration.node_two = node_two;
    }

    public static String getNode_three() {
        return node_three;
    }

    public static void setNode_three(String node_three) {
        MongoConfiguration.node_three = node_three;
    }

    public static String getNode_four() {
        return node_four;
    }

    public static void setNode_four(String node_four) {
        MongoConfiguration.node_four = node_four;
    }

    public static String getNode_zero() {
        return node_zero;
    }

    public static void setNode_zero(String node_zero) {
        MongoConfiguration.node_zero = node_zero;
    }

    public static int getPort_zero() {
        return port_zero;
    }

    public static void setPort_zero(int port_zero) {
        MongoConfiguration.port_zero = port_zero;
    }

    public static int getPort_one() {
        return port_one;
    }

    public static void setPort_one(int port_one) {
        MongoConfiguration.port_one = port_one;
    }

    public static int getPort_two() {
        return port_two;
    }

    public static void setPort_two(int port_two) {
        MongoConfiguration.port_two = port_two;
    }

    public static int getPort_three() {
        return port_three;
    }

    public static void setPort_three(int port_three) {
        MongoConfiguration.port_three = port_three;
    }

    public static int getPort_four() {
        return port_four;
    }

    public static void setPort_four(int port_four) {
        MongoConfiguration.port_four = port_four;
    }

    public static String getDbName() {
        return dbName;
    }

    public static void setDbName(String dbName) {
        MongoConfiguration.dbName = dbName;
    }




}
