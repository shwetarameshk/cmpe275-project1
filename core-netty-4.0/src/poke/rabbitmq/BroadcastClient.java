package poke.rabbitmq;


import com.rabbitmq.client.AMQP;



public class BroadcastClient {
	MQueueFactory factory;

	public BroadcastClient(String host, int port, String user, String password) {
		factory = new MQueueFactory(host, port, user, password);
	}

	public void sendMessage(eye.Comm.Request message) {
		try {
			MQueuePublisher queue = factory.createPublisher("pubsubnetty");
			queue.publish(message,"one");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n * messages sent *");
	}
}
