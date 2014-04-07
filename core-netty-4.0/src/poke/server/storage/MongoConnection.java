package poke.server.storage;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.mongodb.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import poke.server.resources.CourseDetails;
import poke.server.resources.MongoConfiguration;

/**
 * Created by poojasrinivas on 4/4/14.
 */
public class MongoConnection {

    private boolean success = false;
    private MongoClient mongoClient;
    private static DB db;
    private static DBCollection collection;
    private static DBObject course;

    private String dbHost1;
    private String dbHost2;
    private String dbHost3;
    private String dbHost4;
    private String dbHost0;

    private MongoClient mongoClientDAO;

    int dbPort1;
    int dbPort2;
    int dbPort3;
    int dbPort4;
    int dbPort0;

    String dbName;

    public MongoConnection() throws IOException{

        MongoConfiguration configuration = new MongoConfiguration();
        this.dbName = configuration.getDbName();

        this.dbHost0 = configuration.getNode_zero();
        this.dbHost1 = configuration.getNode_one();
        this.dbHost2 = configuration.getNode_two();
        this.dbHost3 = configuration.getNode_three();
        this.dbHost4 = configuration.getNode_four();

        this.dbPort0 = configuration.getPort_zero();
        this.dbPort1 = configuration.getPort_one();
        this.dbPort2 = configuration.getPort_two();
        this.dbPort3 = configuration.getPort_three();
        this.dbPort4 = configuration.getPort_four();

    }


    @SuppressWarnings("finally")
    public boolean obtainConnection(String host, int port) {
        try {
                mongoClient = new MongoClient(Arrays.asList(new ServerAddress(dbHost0,dbPort0),
                        new ServerAddress(dbHost1,dbPort1),new ServerAddress(dbHost2,dbPort2),
                        new ServerAddress(dbHost3,dbPort3),new ServerAddress(dbHost4,dbPort4)));
                       //new MongoClient(new ServerAddress(host, port));

                db = mongoClient.getDB(dbName);

                collection = db.getCollection("courseList");
                success = true;
        }
        catch (UnknownHostException e) {
            System.out.println("Exception at + " + e.getMessage());
            success = false;
            System.out.println(e);
            e.printStackTrace();
        }
        catch(Exception e){
            System.out.println("Exception at + " + e.getStackTrace());
        }
        finally {
            return success;
        }
    }

    public DBCollection getCollection(String collectionName){
        collection = db.getCollection(collectionName);
        return collection;
    }

    public void insertData(BasicDBObject doc)	{
        collection.insert(doc);
    }

    public void deleteData(BasicDBObject doc)	{
        collection.findAndRemove(doc);
    }

    public void updateData(BasicDBObject query, BasicDBObject update )	{
        collection.findAndModify(query, update);
    }

    public void closeConnection() {
        mongoClientDAO.close();
    }

    public DBCursor findData(BasicDBObject query) {
        return collection.find(query);
    }

    @SuppressWarnings("finally")
    public CourseDetails getCourse(String key, String value) {
        CourseDetails course = new CourseDetails();
        // DBObject course = collection.findOne();
        // System.out.println(course);

        BasicDBObject query = new BasicDBObject(key, value);

        DBCursor cursor = collection.find(query);

        try {
            while (cursor.hasNext()) {

                String cour = cursor.next().toString();
                System.out.println(cour);
                JSONParser parser = new JSONParser();

                JSONObject jsonObject = (JSONObject) parser.parse(cour);

                String c_id = (String) jsonObject.get("c_id");
                course.setCourseId(c_id);

                String c_descp = (String) jsonObject.get("c_descp");
                course.setCourseDescp(c_descp);


                String c_prof = (String) jsonObject.get("c_prof");
                course.setCourseProf(c_prof);
            }
        }
        finally {
            cursor.close();
            return course;
        }
    }

    public boolean createCollection(String collectionName) {
        if(db!= null && !db.collectionExists(collectionName))
            db.createCollection(collectionName, null);
        else
            return false;
        return true;

    }

    public boolean insertDocIntoCollection(CourseDetails courseDetails, String collectionName){

        if(db!= null && !db.collectionExists(collectionName))
            collection =  db.createCollection(collectionName, null);
        else
            return false;


        BasicDBObject basicDBObject = new BasicDBObject("c_id", courseDetails.getCourseId()).
                append("c_prof", courseDetails.getCourseProf()).
                append("c_descp", courseDetails.getCourseDescp());

        if(collection != null){
            collection.insert(basicDBObject);
            return true;
        }
        return false;
    }
}
