package poke.server.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by poojasrinivas on 4/2/14.
 */
public class CommanUtils {
    protected static Logger logger = LoggerFactory.getLogger("Util");


    public static int compareNodeId(String first, String two)
    {
        int returnValue = 0;
        int firstvalue = getIntForString(first);
        int secondvalue = getIntForString(two);
        logger.info("***** the values are firstValue :"  + firstvalue + "&& " + secondvalue);
        if(firstvalue > secondvalue)
            return 0;
        else
            return 1;

    }
    public static int getIntForString(String str){

        if(str.equals("one"))  return 1;
        else if(str.equals("zero")) return 0;
        else if(str.equals("two")) return 2;
        else if(str.equals("three")) return 3;
        else if(str.equals("four")) return 4;
        else if(str.equals("five")) return 5;
        else return 0;
    }
}
