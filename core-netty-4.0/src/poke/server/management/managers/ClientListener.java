package poke.server.management.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.util.ClientUtil;
import eye.Comm.Header;

public class ClientListener
{
	protected static Logger logger = LoggerFactory.getLogger("clientlistener");
	
	private String listenerID;
	
	public ClientListener(String id)
	{
		this.listenerID=id;
	}
	
	public String getListenerID() {
		//return data.getNodeId();
		return this.listenerID;
	}
	
	public void onMessage(eye.Comm.Request msg)
	{
		logger.info("In Server onMessage");
		if(msg.hasBody())
		{

			logger.info("Has payload");
		}
		
		
		if (logger.isDebugEnabled())
			ClientUtil.printHeader(msg.getHeader());
		
		if(msg.hasHeader()){

		if (msg.getHeader().getRoutingId().getNumber() == Header.Routing.PING_VALUE)
		{
			logger.info("In Server listener");
			ClientUtil.printPing(msg.getBody().getPing());
		}
		else if (msg.getHeader().getRoutingId().getNumber() == Header.Routing.NAMESPACES_VALUE) {
			logger.info("In namespace value");
			// namespace responses
		} else if (msg.getHeader().getRoutingId().getNumber() == Header.Routing.JOBS_VALUE) {
			logger.info("In job values");
			// job responses
		} else if (msg.getHeader().getRoutingId().getNumber() == Header.Routing.MANAGE_VALUE) {
			logger.info("In management value");
			// management responses
		} else {
			logger.info("Unexpected reply");
			// unexpected reply - how do you handle this?
		}
		}
		else
		{
			logger.info("No header in message");
		}
	}
}