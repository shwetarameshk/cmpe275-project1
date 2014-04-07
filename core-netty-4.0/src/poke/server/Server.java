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
package poke.server;

import com.google.protobuf.GeneratedMessage;
import eye.Comm;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.*;
import java.io.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;

import com.rabbitmq.client.ShutdownSignalException;

import eye.Comm.Header;
import eye.Comm.LeaderElection;
import eye.Comm.Management;
import eye.Comm.Payload;
import eye.Comm.Ping;
import eye.Comm.Request;
import eye.Comm.LeaderElection.VoteAction;

import poke.client.comm.CommHandler;
import poke.client.comm.ClientProducer;
import poke.rabbitmq.MQueueFactory;
import poke.rabbitmq.MQueueSubscriber;
import poke.server.conf.JsonUtil;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.management.ManagementInitializer;
import poke.server.management.ManagementQueue;
import poke.server.management.managers.ElectionManager;
import poke.server.management.managers.ElectionRequest;
import poke.server.management.managers.HeartbeatConnector;
import poke.server.management.managers.HeartbeatData;
import poke.server.management.managers.HeartbeatManager;
import poke.server.management.managers.JobManager;
import poke.server.management.managers.NetworkManager;
import poke.server.management.managers.*;
import poke.server.queue.ChannelQueue;
import poke.server.queue.PerChannelQueue;
import poke.server.queue.QueueFactory;
import poke.server.resources.RabbitMQIP;
import poke.server.resources.ResourceFactory;

/**
 * Note high surges of messages can close down the channel if the handler cannot
 * process the messages fast enough. This design supports message surges that
 * exceed the processing capacity of the server through a second thread pool
 * (per connection or per server) that performs the work. Netty's boss and
 * worker threads only processes new connections and forwarding requests.
 * <p>
 * Reference Proactor pattern for additional information.
 * 
 * @author gash
 * 
 */
public class Server {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static ChannelGroup allChannels;
	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
	protected ServerConf conf;

	protected JobManager jobMgr;
	protected NetworkManager networkMgr;
	protected HeartbeatManager heartbeatMgr;
	protected ElectionManager electionMgr;
	protected ElectionRequest electionReq;
	
	public static ServerInfo serverInfo = new ServerInfo();
	public static LeaderFunctions lf;

    public static String LastModifiedAt;
	

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		try {
			if (allChannels != null) {
				ChannelGroupFuture grp = allChannels.close();
				grp.awaitUninterruptibly(5, TimeUnit.SECONDS);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.info("Server shutdown");
		System.exit(0);
	}

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public Server(File cfg) {
	logger.info("Inside server constructor");
		init(cfg);
	}

	private void init(File cfg) {
		// resource initialization - how message are processed
        logger.info("Inside init");
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), ServerConf.class);
			ServerNodeInfo.nodeId=conf.getServer().getProperty("node.id");
			ResourceFactory.initialize(conf);
		} catch (Exception e) {
		}
	}

	public void release() {
logger.info("Inside release");
		if (HeartbeatManager.getInstance() != null)
			HeartbeatManager.getInstance().release();
	}

	/**
	 * initialize the outward facing (public) interface
	 * 
	 *
	 *            The port to listen to
	 */
	private static class StartCommunication implements Runnable {
		ServerConf conf;
        ChannelQueue queue;

        public StartCommunication(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)
            logger.info("Inside Server-StartCommuication run");

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				String str = conf.getServer().getProperty("port");
				if (str == null) {
					// TODO if multiple servers can be ran per node, assigning a
					// default
					// is not a good idea
					logger.warn("Using default port 5570, configuration contains no port number");
					str = "5570";
				}

				int port = Integer.parseInt(str);

				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(port, b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ServerInitializer(compressComm));

				// Start the server.
				logger.info("Starting server " + conf.getServer().getProperty("node.id") + ", listening on port = "
						+ port);

				ChannelFuture f = b.bind(port).syncUninterruptibly();
                queue = QueueFactory.getInstance(f.channel());
                // should use a future channel listener to do this step
				// allChannels.add(f.channel());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}

			// We can also accept connections from a other ports (e.g., isolate
			// read
			// and writes)
		}
	}
	
	
	//Jeena
	private static class ListenOnPort implements Runnable {
		ServerConf conf;

		public ListenOnPort(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)
			logger.info("Inside Server-StartCommuication run");

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				String str = conf.getServer().getProperty("port");
				if (str == null) {
					// TODO if multiple servers can be ran per node, assigning a
					// default
					// is not a good idea
					logger.warn("Using default port 5570, configuration contains no port number");
					str = "5570";
				}

				int port = Integer.parseInt(str);

				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(port, b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ServerInitializer(compressComm));
				
				//Jeena
				//port=15000;

				// Start the server.
				logger.info("Starting server " + conf.getServer().getProperty("node.id") + ", listening on port = "
						+ port);
				ChannelFuture f = b.bind(port).syncUninterruptibly();
				//f.addListener(new PortListener());
				//ChannelQueue queue = QueueFactory.getInstance(f.channel());
				
				//Request.Builder req=Request.newBuilder();
				//Ping.Builder pingBuild = eye.Comm.Ping.newBuilder();
				//pingBuild.setTag("tag");
				//pingBuild.setNumber(1);
				//eye.Comm.Payload.Builder p = Payload.newBuilder();
				//p.setPing(pingBuild.build());
				//req.setBody(p.build());

				// header with routing info
				//eye.Comm.Header.Builder h = Header.newBuilder();
				//h.setOriginator("client");
				//h.setTag("test finger");
				//h.setTime(System.currentTimeMillis());
				//h.setRoutingId(eye.Comm.Header.Routing.PING);
				//req.setHeader(h.build());
				
				//queue.enqueueRequest(req.build(), f.channel());

				// should use a future channel listener to do this step
				 // allChannels.add(f.channel());
				  
				 //Jeena

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}

			// We can also accept connections from a other ports (e.g., isolate
			// read
			// and writes)
		}
	}
	
	
	
	// To start the consumer
	private static class StartConsumer implements Runnable
	{

		@Override
		public void run() {
			
			
			MQueueFactory factory = new MQueueFactory(RabbitMQIP.rabbitMQIP, AMQP.PROTOCOL.PORT, "guest", "guest");
			//ServerConsumer queue = factory.createConsumer("netty-testing");

			List<eye.Comm.Request> list;
			/*try {
				
				list = queue.retrieve();
				for (eye.Comm.Request msg : list)
					logger.info("msg received: " + msg);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			com.rabbitmq.client.Channel ch=factory.getChannel();
			try {
				ch.queueDeclare("netty-testing", false, false, false, null);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			QueueingConsumer consumer = new QueueingConsumer(ch);
		    try {
				ch.basicConsume("netty-testing", true, consumer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    while (true) {
		      QueueingConsumer.Delivery delivery=null;
			try {
				delivery = consumer.nextDelivery();
			} catch (ShutdownSignalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConsumerCancelledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      byte[] message = delivery.getBody();
		      eye.Comm.Request request=null;
		      try {
				request=eye.Comm.Request.parseFrom(message);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      System.out.println(" [x] Received '" + request + "'");
		      if(lf!=null)
		      {
		    	  lf.forwardRequest(request);
		      }
		    }
			
			
		}
		
	}
	
	
	//Start response Consumer
	private static class StartResponseConsumer implements Runnable
	{

		@Override
		public void run() {
			
			
			MQueueFactory factory = new MQueueFactory(RabbitMQIP.rabbitMQIP, AMQP.PROTOCOL.PORT, "guest", "guest");
			//ServerConsumer queue = factory.createConsumer("netty-testing");

			List<eye.Comm.Request> list;
			/*try {
				
				list = queue.retrieve();
				for (eye.Comm.Request msg : list)
					logger.info("msg received: " + msg);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

            com.rabbitmq.client.Channel ch=factory.getChannel();
			try {
				ch.queueDeclare("netty-response", false, false, false, null);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			QueueingConsumer consumer = new QueueingConsumer(ch);
		    try {
				ch.basicConsume("netty-response", true, consumer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    while (true) {
		      QueueingConsumer.Delivery delivery=null;
			try {
				delivery = consumer.nextDelivery();
			} catch (ShutdownSignalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConsumerCancelledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      byte[] message = delivery.getBody();
		      eye.Comm.Request request=null;
		      try {
				request=eye.Comm.Request.parseFrom(message);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      logger.info("Inside response consumer");
		      System.out.println(" [x] Received in response consumer '" + request + "'");
		      
		      String nodeId=request.getHeader().getToNode();
		      serverInfo.setNodeAvailable(nodeId);
		    }
			
			
		}
		
	}
	
	//Start the subscriber
	private static class StartSubscriber extends poke.rabbitmq.MQueueListener implements Runnable
	{

		private MQueueSubscriber sub;
		private String topic;

		public StartSubscriber(){
			this.topic = ServerNodeInfo.nodeId;
		}
		public void initialize() {
			// in a robust application/server this information would be passed to
			// the factory from a configuration (e.g., from a file).
			String host = RabbitMQIP.rabbitMQIP;
			String user = "test";
			String passwd = "test";

			MQueueFactory factory = new MQueueFactory(host, AMQP.PROTOCOL.PORT, user, passwd);
			sub = factory.createSubscriber("pubsubnetty");
			sub.addListener(this);
		}

		@Override
		public void onMessage(eye.Comm.Request msg, String topic) {
			logger.info("here");
			// TODO should be abstract but for demonstration we print the message
			System.out.println("MSG(" + topic + " - " + msg);
			
			//Once we get the response back we redirect to response producer queue
			MQueueFactory factory = new MQueueFactory("0.0.0.0", AMQP.PROTOCOL.PORT, "guest", "guest");
			ResponseProducer queue = factory.createResponseProducer("netty-response");
			
			try
			{
				queue.post(msg);
			}
			catch(Exception e)
			{
				logger.info("Error in posting to response queue");
				e.printStackTrace();
			}
		}

		public void addBindingFilter(String v) {
			sub.addTopic(v);
		}

		public void demo() {
			System.out.println("demo");
			// blocking
			try {
				sub.subscribe();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		

		@Override
		public void run() {
			initialize();
			addBindingFilter(this.topic);
			demo();
			
		}

	}


	/**
	 * initialize the private network/interface
	 * 
	 *
	 *            The port to listen to
	 */
	private static class StartManagement implements Runnable {
		private ServerConf conf;

		public StartManagement(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			logger.info("Inside Server -Management run");
			// construct boss and worker threads (num threads = number of cores)

			// UDP: not a good option as the message will be dropped

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				String str = conf.getServer().getProperty("port.mgmt");
				int mport = Integer.parseInt(str);

				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(mport, b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ManagementInitializer(compressComm));

				// Start the server.

				logger.info("Starting mgmt " + conf.getServer().getProperty("node.id") + ", listening on port = "
						+ mport);
				ChannelFuture f = b.bind(mport).syncUninterruptibly();

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	/**
	 * this initializes the managers that support the internal communication
	 * network.
	 * 
	 * TODO this should be refactored to use the conf file
	 */
	private void startManagers() {
		if (conf == null)
			return;

		// start the inbound and outbound manager worker threads
		ManagementQueue.startup();


		String myId = conf.getServer().getProperty("node.id");

		// create manager for network changes
		networkMgr = NetworkManager.getInstance(myId);
		logger.info("After network manager");

		// create manager for leader election
		String str = conf.getServer().getProperty("node.votes");
		int votes = 1;
		if (str != null)
			votes = Integer.parseInt(str);
		electionMgr = ElectionManager.getInstance(myId, votes, conf);

		//--Jeena
		
		electionReq = ElectionRequest.getInstance(conf);
		
			/*logger.info("Inside Leader election build");
			
			LeaderElection.Builder h=LeaderElection.newBuilder();
			h.setDesc("Hi I am sending an election msg");
			h.setVote(VoteAction.NOMINATE);
			h.setBallotId("0");
			h.setNodeId("zero");
			
			
			for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) 
			{
				ElectionManager em=ElectionManager.getInstance(nn.getNodeId(),1);
				em.processRequest(h.build());
			}*/
			
			//generateLEReq();
		
		
		//--Jeena


		// create manager for accepting jobs
		jobMgr = JobManager.getInstance(myId);


		// establish nearest nodes and start receiving heartbeats
		heartbeatMgr = HeartbeatManager.getInstance(myId);
		for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) {
            String leaderId = nn.getLeaderId();
            if(leaderId == null)
                leaderId = "one";// TODO - Pooja added have to request for the new leader from the nearest node which that assumes.

			HeartbeatData node = new HeartbeatData(nn.getNodeId(), nn.getHost(), nn.getPort(), nn.getMgmtPort(),nn.getLeaderId());
            //node will have the values of the nearest node details like zero will have one s details
			HeartbeatConnector.getInstance().addConnectToThisNode(node);

		}
		heartbeatMgr.start();

		// manage heartbeatMgr connections
		HeartbeatConnector conn = HeartbeatConnector.getInstance();
		conn.start();

		logger.info("Server " + myId + ", managers initialized");
	}
	
	
	//Jeena
	

	/**
	 * 
	 */
	public void run() {
		if (conf == null) {
			logger.error("Missing configuration file");
			return;
		}

		String myId = conf.getServer().getProperty("node.id");
		logger.info("Initializing server " + myId);

		// storage initialization
		// TODO storage setup (e.g., connection to a database)

		startManagers();

		
		StartManagement mgt = new StartManagement(conf);
		Thread mthread = new Thread(mgt);
		mthread.start();

		StartCommunication comm = new StartCommunication(conf);
        logger.info("Server " + myId + " ready");
		
		Thread cthread = new Thread(comm);
		cthread.start();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java " + Server.class.getClass().getName() + " conf-file");
			System.exit(1);
		}

		File cfg = new File(args[0]);
		if (!cfg.exists()) {
			Server.logger.error("configuration file does not exist: " + cfg);
			System.exit(2);
		}

		Server svr = new Server(cfg);
		svr.run();
        //callfromclient();
		//Initialize hash table

        serverInfo=new ServerInfo();
		lf=new LeaderFunctions(serverInfo);

        if(ServerNodeInfo.isLeader())
		{ 
			/*StartConsumer sc=new StartConsumer();
			Thread conThread=new Thread(sc);
			conThread.start();*/
        	
        	
			
			/*StartResponseConsumer src=new StartResponseConsumer();
			Thread connectionThread=new Thread(src);
			connectionThread.start();*/
			
		}
		else
		{
			/*StartSubscriber ss=new StartSubscriber();
			Thread conThread=new Thread(ss);
			conThread.start();*/
		}
		
		
	}

    public static void callfromclient(){
        try{
        String fromclient;

        ServerSocket javaserver = new ServerSocket(5000);
        logger.info ("TCPServer Waiting for client on port 5000");

        while(true)
        {
            Socket connected = javaserver.accept();
            logger.info( " THE CLIENT"+" "+ connected.getInetAddress() +":"+connected.getPort()+" IS CONNECTED ");

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader (connected.getInputStream()));

            while ( true )
            {
                fromclient = inFromClient.readLine();

                if ( fromclient.equals("q") || fromclient.equals("Q") )
                {
                    connected.close();
                    break;
                }
                else
                {
                    logger.info( "RECIEVED:" + fromclient );
                }
            }
        }
        }
        catch (Exception e){

        }
    }
}
