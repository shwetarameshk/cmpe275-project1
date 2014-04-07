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

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.ServerNodeInfo;
import poke.server.VotingConnection;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.ResourceFactory;

import eye.Comm.JobBid;
import eye.Comm.JobProposal;
import eye.Comm.Management;

/**
 * The job manager class is used by the system to assess and vote on a job. This
 * is used to ensure leveling of the servers take into account the diversity of
 * the network.
 * 
 * @author gash
 * 
 */
public class JobManager {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<JobManager> instance = new AtomicReference<JobManager>();

	private String nodeId;
	
	ServerConf conf;
    VotingConnection vc;
    String nodeIp;

	public static JobManager getInstance(String id) {
		
		instance.compareAndSet(null, new JobManager(id));
		return instance.get();
	}

	public static JobManager getInstance() {
		return instance.get();
	}

	public JobManager(String nodeId) {
		this.nodeId = nodeId;
		this.conf = ResourceFactory.cfg;
		//conf.getServer().getProperty(name)
	}
	
	public void processRequest(Management mgmtReq, JobProposal req) {
		
		logger.info("Job proposal process request");

		//received job proposal from leader
		//create job bid and forward to nearest node 

		Random randomGenerator = new Random();
		int bidValue = randomGenerator.nextInt(1);
		
		JobBid.Builder bidBuilder = eye.Comm.JobBid.newBuilder();
		bidBuilder.setNameSpace(req.getNameSpace());
		bidBuilder.setOwnerId(req.getOwnerId());
		bidBuilder.setJobId(req.getJobId());
		bidBuilder.setBid(bidValue);
		
		eye.Comm.JobBid jobBid = bidBuilder.build();
		
		//forward to nearest node 
		
		forwardJobBid (jobBid);

	}

	/**
	 * a job bid for my job
	 * 
	 * @param req
	 *            The bid
	 */
	public void processRequest(Management mgmtReq, JobBid req) {
		
		logger.info("Job bid process request");
		
		Random randomGenerator = new Random();
		int bidValue = randomGenerator.nextInt(2);
		
		//if not leader - add 1 if yes, add 0 if no
		JobBid.Builder bidBuilder = eye.Comm.JobBid.newBuilder();
		bidBuilder.setNameSpace(req.getNameSpace());
		bidBuilder.setOwnerId(req.getOwnerId());
		bidBuilder.setJobId(req.getJobId());
		bidBuilder.setBid(req.getBid()+bidValue);
		
		eye.Comm.JobBid newJobBid = bidBuilder.build();
		//if not leader - forward to nearest node 
		if (!ServerNodeInfo.isLeader(this.nodeId))
		{
			logger.info("Node not leader. Will forward job bid");
			logger.info("**Job Bid message: " + newJobBid);
			forwardJobBid (newJobBid);
		}
			
		else
		{
			logger.info("Node is leader. Will send init voting");
			sendInitVotingResponse();
		}
		
		
		//if leader - calculate votes and send back yes or no
		//based on final bid value. if bid value >= numOfNodes/2, then return yes, else no
	}

	private void sendInitVotingResponse() {
		// TODO Auto-generated method stub
		eye.Comm.InitVoting.Builder votingResponseBuilder = eye.Comm.InitVoting.newBuilder();
		votingResponseBuilder.setVotingId("1");
		votingResponseBuilder.setHostIp("2");
		votingResponseBuilder.setPortIp("3");
		
	}

	private void forwardJobBid(JobBid newJobBid) {
		
		//Send it to nearest node
		
		for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) 
		{
			logger.info("Nearest node is "+nn.getNodeId());
			logger.info("nearest node host is "+nn.getHost());
			logger.info("Nearest node port is "+nn.getPort() );
			this.vc=new VotingConnection(nn.getHost(),nn.getMgmtPort());
		}
		
		eye.Comm.Management.Builder managementBuilder = eye.Comm.Management.newBuilder();
		managementBuilder.setJobBid(newJobBid);
		eye.Comm.Management message = managementBuilder.build();
		try {
			logger.info("send job bid");
			
			vc.sendMessage(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
