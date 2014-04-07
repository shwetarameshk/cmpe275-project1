package poke.server;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.ServerHandler;
import poke.server.ServerInitializer;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.JobProposal;
import eye.Comm.Management;

	/**
	 * provides an abstraction of the communication to the remote server.
	 * 
	 * @author gash
	 * 
	 */
	public class VotingConnection
	{
		protected static Logger logger = LoggerFactory.getLogger("connect");

		private String host;
		private int port;
		private ChannelFuture channel; // do not use directly call connect()!
		private EventLoopGroup group;
		private ServerHandler serverHandler;
		private JobProposalHandler jpHandler;
		private JobProposal jpMessage;

		// our surge protection using a in-memory cache for messages
		private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;

		// message processing is delegated to a threading model
		private OutboundWorker worker;

		/**
		 * Create a connection instance to this host/port. On consruction the
		 * connection is attempted.
		 * 
		 * @param host
		 * @param port
		 */
		public VotingConnection(String host, int port) {
			this.host = host;
			this.port = port;

			init();
		}

		/**
		 * release all resources
		 */
		public void release() {
			group.shutdownGracefully();
		}
		
		

		/**
		 * send a message - note this is asynchrounous
		 * 
		 * @param req
		 *            The request
		 * @exception An
		 *                exception is raised if the message cannot be enqueued.
		 */
		public void sendMessage(Management req) throws Exception {
			// enqueue message
			logger.info("In send message of votingConnection");
			//this.jpMessage=req;
			outbound.put(req);
			
		}


		
		/**
		 * abstraction of notification in the communication
		 * 
		 * @param listener
		 */
		

		private void init() {
			// the queue to support server-side surging
			outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

			group = new NioEventLoopGroup();
			try {
				
				serverHandler = new ServerHandler();
				jpHandler=new JobProposalHandler();
				JobProposalInitializer ci=new JobProposalInitializer(false);
				ci.setHandler(jpHandler);
				Bootstrap b = new Bootstrap();
				b.group(group).channel(NioSocketChannel.class).handler(ci);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				
				logger.info("Inside init of VotingConnection");

				// Make the connection attempt.
				channel = b.connect(host, port).syncUninterruptibly();
				
				// want to monitor the connection to the server s.t. if we loose the
				// connection, we can try to re-establish it.
				ServerClosedListener ccl = new ServerClosedListener(this);
				channel.channel().closeFuture().addListener(ccl);
				

			} catch (Exception ex) {
				logger.error("failed to initialize the server connection", ex);

			}

			// start outbound message processor
			worker = new OutboundWorker(this);
			worker.start();
		}

		/**
		 * create connection to remote server
		 * 
		 * @return
		 */
		protected Channel connect() {
			// Start the connection attempt.
			if (channel == null) {
				init();
			}

			if (channel.isDone() && channel.isSuccess())
			{
				logger.info("Channel connection is successful --Jeena");
				return channel.channel();
			}
				
			else
				throw new RuntimeException("Not able to establish connection to server");
		}
		
		

		/**
		 * queues outgoing messages - this provides surge protection if the client
		 * creates large numbers of messages.
		 * 
		 * @author gash
		 * 
		 */
		protected class OutboundWorker extends Thread {
			VotingConnection conn;
			boolean forever = true;

			public OutboundWorker(VotingConnection conn) {
				this.conn = conn;

				if (conn.outbound == null)
					throw new RuntimeException("connection worker detected null queue");
			}

			@Override
			public void run() {
				logger.info("Inside run of CommConn");
				Channel ch = conn.connect();
				if (ch == null || !ch.isOpen()) {
					VotingConnection.logger.error("connection missing, no outbound communication");
					return;
				}
				else
				{
					logger.info("channel is "+ch);
					logger.info("channel is not null --jeena");
					//handler=new CommHandler(ch);

				}

				while (true) {
					if (!forever && conn.outbound.size() == 0)
					{
			
						logger.info("Connection outbound size is 0");
						break;
					}

					try {
						// block until a message is enqueued
						GeneratedMessage msg = conn.outbound.take();
						if (ch.isWritable()) {
						///Jeena	//CommHandler handler = conn.connect().pipeline().get(CommHandler.class);
							//handler.setChannel(ch);
							if (!serverHandler.send(ch,msg))
							{
								logger.info("Send message in handler returned false");
								conn.outbound.putFirst(msg);
							}

						} else
						{
							logger.info("Channel is not writable");
							conn.outbound.putFirst(msg);
						}
					} catch (InterruptedException ie) {
						break;
					} catch (Exception e) {
						VotingConnection.logger.error("Unexpected communcation failure", e);
						break;
					}
				}

				if (!forever) {
					VotingConnection.logger.info("connection queue closing");
				}
			}
		}

		/**
		 * usage:
		 * 
		 * <pre>
		 * channel.getCloseFuture().addListener(new ClientClosedListener(queue));
		 * </pre>
		 * 
		 * @author gash
		 * 
		 */
		public static class ServerClosedListener implements ChannelFutureListener {
			VotingConnection cc;

			public ServerClosedListener(VotingConnection cc) {
				this.cc = cc;
			}

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				// we lost the connection or have shutdown.

				// @TODO if lost, try to re-establish the connection
			}
		}
	}

