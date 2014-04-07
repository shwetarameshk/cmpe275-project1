package poke.rabbitmq;

public class MQueueListener {

	/**
	 * Override this method to provide application processing of a message
	 * 
	 * @param msg
	 */
	public void onMessage(eye.Comm.Request msg, String topic) {
		// TODO should be abstract but for demonstration we print the message
		System.out.println("MSG (" + topic + "): " + msg);
	}
}
