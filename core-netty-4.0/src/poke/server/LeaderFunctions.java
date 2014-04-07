package poke.server;

import com.rabbitmq.client.AMQP;

import poke.rabbitmq.BroadcastClient;
import poke.server.resources.RabbitMQIP;
import eye.Comm.Request;

public class LeaderFunctions
{
	ServerInfo serverInfo;
	
	public LeaderFunctions(ServerInfo serverInfo)
	{
		this.serverInfo=serverInfo;
	}
	
	public void forwardRequest (eye.Comm.Request req){
		// if leader
		String forwardNodeId = new String();	
		
		//serverInfo.initializeNodeInfoTable();
		
		for (String key : serverInfo.getNodeInfoTable().keySet() ){
			if (serverInfo.getNodeInfoTable().get(key).equalsIgnoreCase("available")){
				forwardNodeId = new String (key);
				break;
			}
		}
		serverInfo.setNodeBusy(forwardNodeId);
		//Now forward request
		
		//Start publisher
		Request.Builder r2 = req.toBuilder();
		r2.setBody(req.getBody());
		//r2.setHeader(req.getHeader());
		
		eye.Comm.Header h2 = req.getHeader();
	
		
		
		eye.Comm.Header.Builder hb2 = h2.newBuilder();
		
		hb2.setOriginator(h2.getOriginator());
		hb2.setTag(h2.getTag());
		hb2.setTime(h2.getTime());
		hb2.setRoutingId(h2.getRoutingId());
		hb2.setToNode(forwardNodeId);		
		
		r2.setHeader(hb2.build());
		Request newReq = r2.build();
		 /* ***********************/
		
		System.out.println("***************");
	
		
		String host = RabbitMQIP.rabbitMQIP;
		String user = "test";
		String passwd = "test";
		BroadcastClient client = new BroadcastClient(host, AMQP.PROTOCOL.PORT, user, passwd);
		client.sendMessage(newReq);


	}
	
	public void getNodeResponse (){
		String responseNodeId = new String();
		serverInfo.setNodeAvailable(responseNodeId);
	}
}