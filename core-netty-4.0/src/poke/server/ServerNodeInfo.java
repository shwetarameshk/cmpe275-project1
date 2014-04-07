package poke.server;

import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.io.*;

public class ServerNodeInfo {
	
	public static String nodeId = new String();
    public static String leaderId;
    public static long lastModifiedDate =0;
    public static String whoami;
    public static String myport;
	
    public static boolean isLeader(String nearestNodeId){

        leaderId = getLeaderId();
        if(nearestNodeId.equals(leaderId)){
            return true;
        }
        return false;
    }
    
    public static boolean isLeader(){

        leaderId = getLeaderId();
        if(nodeId.equals(leaderId)){
            return true;
        }
        return false;
    }

    public static String getLeaderId()
    {
        try{
           long currentLastModifiedTime = getLastModifiedDate();
            //here we have currentLastModifiedTime to be the whats the current LMT of the file, and we previously store
            //lastModifiedDate and check if it has been changed.
           if(lastModifiedDate != currentLastModifiedTime){

                accessFile();
            }
           return leaderId;
            }
        catch (Exception e){
            System.out.println("Exception in getting Leader");
            return  null;
        }

    }

    private static long getLastModifiedDate(){
        File file;
        try{
        	file = new File("src/leader.txt");
        return file.lastModified();
        }
        catch (Exception e){
            return 0;
        }
    }

    public static void accessFile() throws IOException {
        String[] leader;
        BufferedReader br = null;
        try {
            String sCurrentLine;
            br = new BufferedReader(new FileReader("src/leader.txt"));
            while ((sCurrentLine = br.readLine()) != null) {
                System.out.println(sCurrentLine);
                leader = sCurrentLine.split(":");
                leaderId = leader[1];
            }
            lastModifiedDate = getLastModifiedDate();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            if(br != null) {
                br.close();
            }

        }

    }

    public static void writeLeaderIntoFile(String winnerNode) throws IOException{
        if(!winnerNode.equals(leaderId)){//only when the current winnerNode from already present leaderNode we write into the file.
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter("src/leader.txt"));
            out.write("Leader:" + winnerNode);
            lastModifiedDate = getLastModifiedDate();
            leaderId = winnerNode;
        } catch (IOException e) {}
        finally {
            if(out != null){
                out.close();
            }
        }
      }
    }

}
