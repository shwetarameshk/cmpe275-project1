//Jeena - 4/6

package poke.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

import poke.server.queue.ChannelQueue;
import eye.Comm.JobProposal;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class JobProposalHandler extends SimpleChannelInboundHandler<eye.Comm.Management>
{

	protected static Logger logger = LoggerFactory.getLogger("server-JobProposalHandler");

	private ChannelQueue queue;
    protected ConcurrentMap<String, Listner> listeners = new ConcurrentHashMap<String, Listner>();

	public JobProposalHandler() {
		logger.info("** Job proposal Handler created **");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, eye.Comm.Management arg1)  
	{
		try{
		logger.info("**** Server got a job proposal!****");
		}
		catch(Exception ex )
		{
			logger.info("exception in read of job proposal handler");
			ex.printStackTrace();
		}
	}
	
	
	
		
}