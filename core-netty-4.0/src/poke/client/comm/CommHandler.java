/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.client.comm;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.rabbitmq.MQueueFactory;
import poke.server.management.managers.ClientListener;
import poke.server.management.managers.HeartbeatListener;
import poke.server.resources.RabbitMQIP;

import com.google.protobuf.GeneratedMessage;
import com.rabbitmq.client.AMQP;

public class CommHandler extends SimpleChannelInboundHandler<eye.Comm.Request> {
	protected static Logger logger = LoggerFactory.getLogger("connect");
	protected ConcurrentMap<String, CommListener> listeners = new ConcurrentHashMap<String, CommListener>();
	protected ConcurrentMap<String,HeartbeatListener> testListeners=new ConcurrentHashMap<String,HeartbeatListener>();
	//protected ConcurrentMap<String,JobProposalListener> test=new ConcurrentHashMap<String,JobProposalListener>();
	protected ConcurrentMap<String,ClientListener> pleaseWork=new ConcurrentHashMap<String,ClientListener>();
	private volatile Channel channel;

	public CommHandler() {
		
	}
	
	public CommHandler(Channel ch)
	{
		this.channel=ch;
	}
	
	public void setChannel(Channel ch)
	{
		logger.info("Setting channel ");
		this.channel=ch;
	}

	
	/**
	 * messages pass through this method. We use a blackbox design as much as
	 * possible to ensure we can replace the underlining communication without
	 * affecting behavior.
	 * 
	 * @param msg
	 * @return
	 */
	public boolean send(GeneratedMessage msg) {
		logger.info("Message is "+msg);
		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		logger.info("Here!! " );
		if(channel==null)
		{
			logger.info("Channel is null");
		}
		//ChannelFuture cf = channel.write(msg); 
		//addListener(hb);
		//addListener(c);
		//addListener(clientListener); 
		
		ChannelFuture ch=channel.writeAndFlush(msg);
		logger.info("Channel is "+channel);
		logger.info("wrote message to channel");
		
		
		if (ch.isDone() && !ch.isSuccess()) {
			logger.info("Failed!!!");
			logger.error("failed to poke!");
			return false;
		}
		
		/*MQueueFactory factory = new MQueueFactory(RabbitMQIP.rabbitMQIP, AMQP.PROTOCOL.PORT, "guest", "guest");
		ClientProducer queue = factory.createProducer("netty-testing");
		try {
			queue.post(msg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		logger.info("message sent");

		return true;
	}

	/**
	 * Notification registration. Classes/Applications receiving information
	 * will register their interest in receiving content.
	 * 
	 * Note: Notification is serial, FIFO like. If multiple listeners are
	 * present, the data (message) is passed to the listener as a mutable
	 * object.
	 * 
	 * @param listener
	 */
	public void addListener(CommListener listener) {
		if (listener == null)
			return;

		listeners.putIfAbsent(listener.getListenerID(), listener);
		
	}
	
	public void addListener(HeartbeatListener listener) {
		if (listener == null)
		{
			logger.info("Heartbeat listener null -Jeena");
			return;
		}
		logger.info("Adding heart beat listener - Jeena");
		logger.info("Listener id is "+listener.getListenerID());
		logger.info("Listener is "+listener);
		testListeners.putIfAbsent(listener.getListenerID(), listener);
	}
	
	/*
	public void addListener(JobProposalListener listener) {
		if (listener == null)
		{
			logger.info("Heartbeat listener null -Jeena");
			return;
		}

		test.putIfAbsent(listener.getListenerID(), listener);
	}*/
	
	public void addListener(ClientListener listener) {
		if (listener == null)
		{
			logger.info("Heartbeat listener null -Jeena");
			return;
		}

		pleaseWork.putIfAbsent(listener.getListenerID(), listener);
	}
	

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, eye.Comm.Request msg) throws Exception {
		for (String id : listeners.keySet()) {
			CommListener cl = listeners.get(id);

			// TODO this may need to be delegated to a thread pool to allow
			// async processing of replies
			cl.onMessage(msg);
		}
	}
}
