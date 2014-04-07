package poke.server.management.managers;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import eye.Comm.LeaderElection;
import eye.Comm.Management;
import eye.Comm.LeaderElection.VoteAction;

public class ElectionRequest
{
	protected static Logger logger = LoggerFactory.getLogger("server");
	
	ServerConf conf;
	
	protected static AtomicReference<ElectionRequest> instance = new AtomicReference<ElectionRequest>();
	
	public static ElectionRequest getInstance(ServerConf conf) {
		instance.compareAndSet(null, new ElectionRequest(conf));
		return instance.get();
	}
	
	public static ElectionRequest getInstance() {
		return instance.get();
	}
	
	protected ElectionRequest(ServerConf conf) {
		this.conf=conf;
	}

	
	public Management generateLEReq(String hbInfo)
	{
		String nodeId="zero";
		logger.info("Inside Leader election build");
		
		logger.info(hbInfo);
		
		String[] parts=hbInfo.split(":");
		String port = parts[1];
		if(port.equals("5670"))
		{
			nodeId="zero";
		}
		else if(port.equals("5671"))
		{
			nodeId="one";
		}
		else if(port.equals("5673"))
		{
			nodeId="two";
		}
		
		LeaderElection.Builder h=LeaderElection.newBuilder();
		h.setDesc("This is an election msg");
		h.setVote(VoteAction.ELECTION);
		h.setBallotId("five");
		h.setNodeId(nodeId);

		Management.Builder m=Management.newBuilder();
		m.setElection(h.build());
		return m.build();
		
		
		/*for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) 
		{
			logger.info("Nearest node is "+nn.getNodeId());
			ElectionManager em=new ElectionManager(nn.getNodeId(),1);
			em.processRequest(h.build());
		}*/
	}
}