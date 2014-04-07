package poke.client.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.rabbitmq.MQueueBase;

import com.google.protobuf.GeneratedMessage;
import com.rabbitmq.client.Channel;

public class ClientProducer extends MQueueBase
{
	protected static Logger logger = LoggerFactory.getLogger("client");
	
	public ClientProducer(Channel channel, String queue) throws Exception {
		super(channel);
		setQueueBasename(queue);

		init();
	}

	private void init() throws Exception {

		// whoa! shouldn't we use log4j here?!
		logger.info("--> declaring: " + getExchange());

		// declare messages are to be sent directly (not fanout or topic)
		channel.exchangeDeclare(getExchange(), "direct", true);

		channel.queueDeclare(getQueue(), false, false, false, null);
		
		channel.queueBind(getQueue(), getExchange(), getRouting());
	}

	public void post(GeneratedMessage msg) throws Exception {
		synchronized (channel) {
			channel.basicPublish("", getQueue(), null, msg.toByteArray());
		}
	}

	public void publish(String msg) throws Exception {
		synchronized (channel) {
			channel.basicPublish(getExchange(), getRouting(), null, msg.getBytes());
		}
	}
}