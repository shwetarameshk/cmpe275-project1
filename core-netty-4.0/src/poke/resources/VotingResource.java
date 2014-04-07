package poke.resources;

import eye.Comm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poke.server.Server;
import poke.server.ServerNodeInfo;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.MongoConnection;

public class VotingResource  implements Resource {

    protected static Logger logger = LoggerFactory.getLogger("server-VotingResource");

    private ServerConf cfg;

    public ServerConf getCfg() {
        return cfg;
    }

    public void setCfg(ServerConf cfg) {
        this.cfg = cfg;
    }

    @Override
    public Comm.Request process(Comm.Request request) {

        logger.info("In Voting Resource" + request);
        try{

        MongoConnection mongoConnection = new MongoConnection();

        if(mongoConnection.obtainConnection())
            logger.info("-------> Connection Established.");
        }

        catch (Exception e){

        }

        // Either can be from Client with InitVoting


        // from a different Cluster requesting the FindLeader

        Comm.Request request1 = ResourceUtil.buildVotingIsLeader(ServerNodeInfo.whoami, ServerNodeInfo.myport);
        return request1;
    }
}
