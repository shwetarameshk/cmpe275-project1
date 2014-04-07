package poke.rabbitmq;

import com.rabbitmq.client.Channel;

public class MQueuePublisher extends MQueueBase {

	MQueuePublisher(Channel channel, String queue) throws Exception {
		super(channel);
		setQueueBasename(queue);

		init();
	}

	private void init() throws Exception {
		
		System.out.println("publisher init");

		// declare message messages are to be sent to a general topic
		channel.exchangeDeclare(getExchange(), "topic");
	}

	/**
	 * publish with no topic (binding)
	 * 
	 * @param msg
	 * @throws Exception
	 */
	public void publish(String msg) throws Exception {
		System.out.println("publish");
		
		synchronized (channel) {
			channel.toString();
			channel.basicPublish(getExchange(), "", null, msg.getBytes());
			channel.toString();
		}
		if (channel.isOpen())
			System.out.println("channel open");
		else
			System.out.println("channel not open");
		channel.toString();
		System.out.println("publish done");
		
	}

	/**
	 * publish with no topic (binding)
	 * 
	 * @param msg
	 * @throws Exception
	 */
	public void publish(eye.Comm.Request msg, String topic) throws Exception {
		System.out.println("publish");
		
		synchronized (channel) {
			channel.basicPublish(getExchange(), topic, null, msg.toByteArray());
		}
		
		System.out.println("publish done");
	}
}
