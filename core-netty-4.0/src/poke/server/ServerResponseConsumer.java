package poke.server;

import java.util.ArrayList;
import java.util.List;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import poke.rabbitmq.*;

public class ServerResponseConsumer extends MQueueBase
{
	private String queue;

	public ServerResponseConsumer(Channel channel, String queue) {
		super(channel);
		setQueueBasename(queue);
	}

	public List<eye.Comm.Request> retrieve() throws Exception {
		ArrayList<eye.Comm.Request> r = new ArrayList<eye.Comm.Request>();
		synchronized (channel) {
			while (true) {
				// note the consumer does a manual ack to recover from errors
				GetResponse response = channel.basicGet(getQueue(), false);
				if (response == null)
					break;

				try {
					AMQP.BasicProperties props = response.getProps();
					byte[] body = response.getBody();
					eye.Comm.Request req=eye.Comm.Request.parseFrom(body);
					if(req!=null)
					{
						r.add(req);
					}
					long deliveryTag = response.getEnvelope().getDeliveryTag();
					channel.basicAck(deliveryTag, false);
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}

		return r;
	}
}