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
package poke.server.management.managers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import poke.monitor.HeartMonitor;
import poke.monitor.MonitorHandler;
import poke.server.ServerNodeInfo;
import poke.server.management.managers.HeartbeatData.BeatStatus;

/**
 * The connector collects connection monitors (e.g., listeners implement the
 * circuit breaker) that maintain HB communication between nodes (to
 * client/requester).
 * 
 * @author gash
 * 
 */
public class HeartbeatConnector extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management-HeartbeatConnector");
	protected static AtomicReference<HeartbeatConnector> instance = new AtomicReference<HeartbeatConnector>();

	private ConcurrentLinkedQueue<HeartMonitor> monitors = new ConcurrentLinkedQueue<HeartMonitor>();
	private int sConnectRate = 2000; // msec
	private boolean forever = true;
    private boolean startedElection = false;
    String[] leader;

	public static HeartbeatConnector getInstance() {
		instance.compareAndSet(null, new HeartbeatConnector());
		return instance.get();
	}

	/**
	 * The connector will only add nodes for connections that this node wants to
	 * establish. Outbound (we send HB messages to) requests do not come through
	 * this class.
	 * 
	 * @param node
	 */
	public void addConnectToThisNode(HeartbeatData node) {
		// null data is not allowed
		if (node == null || node.getNodeId() == null)
			throw new RuntimeException("Null nodes or IDs are not allowed");

		// register the node to the manager that is used to determine if a
		// connection is usable by the public messaging
		HeartbeatManager.getInstance().addAdjacentNode(node);

		// this class will monitor this channel/connection and together with the
		// manager, we create the circuit breaker pattern to separate
		// health-status from usage.
		HeartMonitor hm = new HeartMonitor(node.getNodeId(), node.getHost(), node.getMgmtport(), node.getLeaderId());
		hm.addListener(new HeartbeatListener(node));

		// add monitor to the list of adjacent nodes that we track
		monitors.add(hm);
	}

	@Override
	public void run() {
        int counter = 0;
		if (monitors.size() == 0) {
			logger.info("HB connection monitor not started, no connections to establish");
			return;
		} else
			logger.info("HB connection monitor starting, node has " + monitors.size() + " connections");

		while (forever) {
			try {
				Thread.sleep(sConnectRate);

				// try to establish connections to our nearest nodes
				for (HeartMonitor hb : monitors) {
					if (!hb.isConnected()) {
						try {
							logger.info("attempting to connect to node: " + hb.getNodeInfo() );
							hb.startHeartbeat(hb.getNodeInfo());
                            counter ++;
                            if(counter > 5){
                               /* try {
                                    String sCurrentLine;
                                    BufferedReader br = new BufferedReader(new FileReader("src/leader.txt"));
                                    while ((sCurrentLine = br.readLine()) != null) {
                                        System.out.println(sCurrentLine);
                                        logger.info("kal leader : " + sCurrentLine);
                                        leader = sCurrentLine.split(":");
                                    }
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }*/

                                //logger.info("The node and leader == >" + leader[1] + "==>" + hb.getPortId());
                                logger.info("The leader and nodeid == >" + ServerNodeInfo.getLeaderId() + "==>" + ServerNodeInfo.nodeId);
                                if( !startedElection && ServerNodeInfo.isLeader()){
                                    logger.info("<-----------------Starting elections------------------>");
                                    HeartbeatManager.getInstance().declareElection = true;
                                    startedElection = true;//TODO reset the startedElection value and declare election
                                }
                            }
						} catch (Exception ie) {
							// do nothing
							logger.info("Jeena - Exception in HeartbeatConnector run");
							
						}
					}

				}
			} catch (InterruptedException e) {
				logger.error("Unexpected HB connector failure", e);
				break;
			}
		}
		logger.info("ending heartbeatMgr connection monitoring thread");
	}

	private void validateConnection() {
		// validate connections this node wants to create
		for (HeartbeatData hb : HeartbeatManager.getInstance().incomingHB.values()) {
			// receive HB - need to check if the channel is readable
			if (hb.channel == null) {
				if (hb.getStatus() == BeatStatus.Active || hb.getStatus() == BeatStatus.Weak) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			} else if (hb.channel.isOpen()) {
				if (hb.channel.isWritable()) {
					if (System.currentTimeMillis() - hb.getLastBeat() >= hb.getBeatInterval()) {
						hb.incrementFailures();
						hb.setStatus(BeatStatus.Weak);
					} else {
						hb.setStatus(BeatStatus.Active);
						hb.setFailures(0);
					}
				} else
					hb.setStatus(BeatStatus.Weak);
			} else {
				if (hb.getStatus() != BeatStatus.Init) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			}
		}

		// validate connections this node wants to create
		for (HeartbeatData hb : HeartbeatManager.getInstance().outgoingHB.values()) {
			// emit HB - need to check if the channel is writable
			if (hb.channel == null) {
				if (hb.getStatus() == BeatStatus.Active || hb.getStatus() == BeatStatus.Weak) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			} else if (hb.channel.isOpen()) {
				if (hb.channel.isWritable()) {
					if (System.currentTimeMillis() - hb.getLastBeat() >= hb.getBeatInterval()) {
						hb.incrementFailures();
						hb.setStatus(BeatStatus.Weak);
					} else {
						hb.setStatus(BeatStatus.Active);
						hb.setFailures(0);
					}
				} else
					hb.setStatus(BeatStatus.Weak);
			} else {
				if (hb.getStatus() != BeatStatus.Init) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			}
		}
	}
}
