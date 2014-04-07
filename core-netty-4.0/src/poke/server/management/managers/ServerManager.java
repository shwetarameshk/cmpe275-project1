package poke.server.management.managers;

import eye.Comm;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poke.monitor.MonitorHandler;
import poke.monitor.MonitorInitializer;
import poke.monitor.MonitorListener;
import poke.server.ServerHandler;
import poke.server.ServerInitializer;
import poke.server.ServerListener;

import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by poojasrinivas on 4/2/14.
 */
public class ServerManager extends Thread {
    protected static Logger logger = LoggerFactory.getLogger("server-ServerManager");
    protected static AtomicReference<ServerManager> instance = new AtomicReference<ServerManager>();
    private EventLoopGroup group = new NioEventLoopGroup();
    private List<ServerListener> listeners = new ArrayList<ServerListener>();
    String nodeId;
//    /ManagementQueue mqueue;
    boolean forever = true;

    public ConcurrentHashMap<String, Channel> incomingHB = new ConcurrentHashMap<String, Channel>();

    public static ServerManager getInstance(String id) {
        instance.compareAndSet(null, new ServerManager(id));
        return instance.get();
    }

    public static ServerManager getInstance() {
        return instance.get();
    }


    protected ServerManager(String nodeId) {
        this.nodeId = nodeId;
        //outgoingHB = new DefaultChannelGroup();
    }

    public void addAdjacentNodeChannel(String host, int port, Channel channel) {

        logger.info("The channel created s" + channel);
        incomingHB.put(host,channel);

    }

    protected Channel connect(String host, Integer port, HeartbeatData node)
    {
        try
            {
                ChannelFuture channel;
                ServerHandler handler = new ServerHandler();
                ServerInitializer mi = new ServerInitializer(false);

                Bootstrap b= new Bootstrap() ;

                b.group(group).channel(NioSocketChannel.class).handler(mi);
                b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
                b.option(ChannelOption.TCP_NODELAY, true);
                b.option(ChannelOption.SO_KEEPALIVE, true);


                // Make the connection attempt.
                channel = b.connect(host, port).syncUninterruptibly();
                channel.awaitUninterruptibly(5000l);
                channel.channel().closeFuture().addListener(new CloseListener(node));

                // add listeners waiting to be added
                if (listeners.size() > 0) {
                    for (ServerListener ml : listeners)
                        handler.addListener(ml);
                    listeners.clear();
                }
                return channel.channel();


            }
            catch (Exception ex) {
                logger.debug("failed to initialize the public connection");
                return null;
            }

        }


    public class CloseListener implements ChannelFutureListener {
        private HeartbeatData heart;

        public CloseListener(HeartbeatData heart) {
            this.heart = heart;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (incomingHB.containsValue(heart)) {
                logger.warn("HB incoming channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
                incomingHB.remove(future.channel());
            }
        }
    }
}
