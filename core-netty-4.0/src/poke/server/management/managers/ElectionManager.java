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

import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.GeneratedMessage;
import eye.Comm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


import eye.Comm.LeaderElection;
import eye.Comm.LeaderElection.VoteAction;
import poke.resources.ForwardResource;
import poke.server.ServerNodeInfo;
import poke.server.Util.CommanUtils;
import poke.server.conf.ServerConf;


/**
 * The election manager is used to determine leadership within the network.
 * 
 * @author gash
 * 
 */
public class ElectionManager extends Thread{
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();
    ServerConf conf;
	private String nodeId;
    private int counter = 0;


	/** @brief the number of votes this server can cast */
	private int votes = 1;

	public static ElectionManager getInstance(String id, int votes,ServerConf conf) {
		instance.compareAndSet(null, new ElectionManager(id, votes,conf));
		return instance.get();
	}

	public static ElectionManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the manager for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	public ElectionManager(String nodeId, int votes, ServerConf conf) {
		this.nodeId = nodeId;
        this.conf = conf;

		if (votes >= 0)
			this.votes = votes;
	}

	/*
	 * @param args
	 */
	public void processRequest(Comm.Management recMsgs, LeaderElection req) {
		if (req == null)
			return;

        if(counter >3)return;

		if (req.hasExpires()) {
			long ct = System.currentTimeMillis();
			if (ct > req.getExpires()) {
				// election is over
				return;
			}
		}
		if (req.getVote().getNumber() == VoteAction.ELECTION_VALUE) {
			logger.info("I am hereeeeee");
			logger.info("Election declared by "+req.getNodeId());
			// an election is declared!
		}
        else if (req.getVote().getNumber() == VoteAction.DECLAREVOID_VALUE) {
			// no one was elected, I am dropping into standby mode`
		}
        else if (req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE) {

            counter++;
            String val = req.getNodeId();
            logger.info("Inside Winner --->" + val);
            boolean sent = nominateWinner(val, nodeId);

		} else if (req.getVote().getNumber() == VoteAction.ABSTAIN_VALUE) {
			// for some reason, I decline to vote
		}
        else if (req.getVote().getNumber() == VoteAction.NOMINATE_VALUE) {
			logger.info("Inside Nominate");
			logger.info("My value: "+ nodeId);
			logger.info("Request node id : "+req.getNodeId());
            int comparedToMe = CommanUtils.compareNodeId(nodeId,req.getNodeId());
			//int comparedToMe = req.getNodeId().compareTo(nodeId);
			logger.info("value of compared "+comparedToMe);

            if (comparedToMe > 0) {
                logger.info("value of compared "+comparedToMe);
				forwardElection(req.getNodeId(), nodeId);
			}else if (comparedToMe <=  0 ) {
                forwardElection(nodeId, nodeId);
            }
		}
	}

    public static void forwardElection(String electedNode, String nodeId){
        boolean sent = false;
        logger.info("Elected node is :"+electedNode);
        try{
            LeaderElection.Builder leaderBuilder = LeaderElection.newBuilder();
            leaderBuilder.setVote(VoteAction.NOMINATE);
            leaderBuilder.setBallotId("five");
            leaderBuilder.setDesc("I am desc from nominate");
            leaderBuilder.setNodeId(electedNode);

            Comm.Management.Builder builder = Comm.Management.newBuilder();
            builder.setElection(leaderBuilder.build());

            GeneratedMessage msg = builder.build();

            for (HeartbeatData hd : HeartbeatManager.getInstance().outgoingHB.values()) {
                if(!hd.channel.isOpen()){
                    nominateWinner(electedNode, nodeId);
                    sent = false;
                    break;
                }
                sent = true;
                if (hd.getFailuresOnSend() > HeartbeatData.sFailureToSendThresholdDefault)
                    continue;

                try {
                    logger.info("Sending election msg:" + msg);
                    logger.info("sending nominate value " + hd.getNodeId()+ " at" + hd.getPort()+ " and the node id is " + nodeId);
                    hd.channel.writeAndFlush(msg);

                } catch (Exception e) {
                    hd.incrementFailuresOnSend();
                }
            }
            if(!sent){
                nominateWinner(electedNode,nodeId);
            }

            /*for (String hd : ServerManager.getInstance(nodeId).incomingHB.keySet()) {
                try{
                    logger.info("the value of the channel is "+ ServerManager.getInstance(nodeId).incomingHB.get(hd));
                    ServerManager.getInstance(nodeId).incomingHB.get(hd).writeAndFlush(msg);
                }
                catch (Exception e){
                    logger.info("exception at servermanager sending elections" + e);
                }
            }*/
        }
        catch (Exception e)
        {
            logger.info("exception in sending the election " + e);
        }
    }

    public static boolean nominateWinner(String winnerNode, String nodeId){
        logger.info("<-------------------------NominateWinner----> :"+ winnerNode);

        try{
            ServerNodeInfo.writeLeaderIntoFile(winnerNode);
        }
        catch (Exception e){
            logger.info("Exception in writing the leader into file");
        }

        LeaderElection.Builder leaderBuilder = LeaderElection.newBuilder();
        leaderBuilder.setVote(VoteAction.DECLAREWINNER);
        leaderBuilder.setBallotId("five");
        leaderBuilder.setDesc("I am desc from nominate");
        leaderBuilder.setNodeId(winnerNode);

        Comm.Management.Builder builder = Comm.Management.newBuilder();
        builder.setElection(leaderBuilder.build());

        GeneratedMessage msg = builder.build();

        for (String hd : ServerManager.getInstance(nodeId).incomingHB.keySet()) {
            if(!ServerManager.getInstance(nodeId).incomingHB.get(hd).isOpen())
                return false;
            try{
                logger.info("the value of the channel is "+ ServerManager.getInstance(nodeId).incomingHB.get(hd));
                ServerManager.getInstance(nodeId).incomingHB.get(hd).writeAndFlush(msg);
            }
            catch (Exception e){
                logger.info("exception at servermanager sending elections" + e);
            }

    }
        return true;
   }
}
