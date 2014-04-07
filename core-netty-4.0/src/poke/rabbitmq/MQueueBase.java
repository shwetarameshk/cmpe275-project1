package poke.rabbitmq;

import com.rabbitmq.client.Channel;

public class MQueueBase {
	
	protected Channel channel;
	private String queue;
	private String routing;
	private String exchange;

	public MQueueBase(Channel channel) {
		this.channel = channel;
	}

	public void setQueueBasename(String base) {
		this.queue = base;
		this.routing = base + "-routing";
		this.exchange = base + "-exchange";
	}

	public void close() throws Exception {
		if (channel != null) {
			channel.close();
		}
	}

	public String getQueue() {
		return queue;
	}

	public String getRouting() {
		return routing;
	}

	public String getExchange() {
		return exchange;
	}
}