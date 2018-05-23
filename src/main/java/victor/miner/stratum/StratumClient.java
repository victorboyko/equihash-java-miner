package victor.miner.stratum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

import static victor.miner.equihash.EquihashUtil.*;

import victor.miner.PoolCredentials;
import victor.miner.equihash.Cancelable;
import victor.miner.equihash.HashMapSolver;
import victor.miner.equihash.Solver;
import victor.miner.stratum.in.InMessage;
import victor.miner.stratum.in.NotifyMessage;
import victor.miner.stratum.in.SetTargetMessage;
import victor.miner.stratum.in.SubscribeRsMessage;
import victor.miner.stratum.out.AuthoriseMessage;
import victor.miner.stratum.out.OutMessage;
import victor.miner.stratum.out.SubmitMessage;
import victor.miner.stratum.out.SubscribeMessage;

public class StratumClient {
	
	private static final Logger logger = Logger.getLogger(StratumClient.class);
	
	private PoolCredentials creds;
	private int nextMessageIndex = 3;
	private Map<Integer, InMessage> expected = new HashMap<>();
	
	public StratumClient(PoolCredentials creds) {
		this.creds = creds;
	}
	
	private Socket socket;
	private PrintWriter pw;
	private BufferedReader br;
	
	private void openConnection() throws IOException {
		socket = new Socket(creds.host, creds.port);
		pw = new PrintWriter(socket.getOutputStream());
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	private String nonceRight = null;
	private int nonceCnt = 0;
	private void resetMining() {
		if (solver instanceof Cancelable) {
			((Cancelable)solver).cancel();
		}
		solver = new HashMapSolver();
		Solver curSolver = solver;
			while(curSolver == solver) {
			nonceRight = String.valueOf(++nonceCnt);
			nonceRight = "00000000".substring(nonceRight.length()) + nonceRight;
			String jobId = job_id;
			String workStr = nversion + hash_prev_block + hash_merkle_root + hash_reserved + ntime + nbits + nonceLeft + nonceRight;
			String curTarget = target;
			String curNtime = ntime;
			String curNonceRight = nonceRight;
			List<String> solutions = solver.solve(nversion, hash_prev_block, hash_merkle_root, hash_reserved, ntime, nbits, nonceLeft, nonceRight);
			List<String> shares = getSharesFromSolutions(solutions, workStr, curTarget);
			System.out.println("Shares : " + shares.size());
			for(String share : shares) {
				sendMessage(new SubmitMessage(nextMessageIndex++, creds.username, jobId, curNtime, curNonceRight, share));
			}

		}
	}
	
	private Solver solver = new HashMapSolver();
	private String nonceLeft;
	private String target;
	private String job_id, nversion, hash_prev_block, hash_merkle_root, hash_reserved, ntime, nbits;
	
	private void startConversationThread() {
		new Thread(()->{
			String line;
			try {
				while ((line = br.readLine()) != null) {
					logger.debug("--<<--:" + line);

					InMessage inMessage = InMessage.parse(line, expected);
					if (inMessage.getErrorMessage() != null) {
						throw new IllegalStateException(inMessage.getErrorCode() + " : " + inMessage.getErrorMessage());
					}
					if (inMessage instanceof SubscribeRsMessage) {
						nonceLeft = ((SubscribeRsMessage)inMessage).getNonceLeftPart();
					}
					if (inMessage instanceof SetTargetMessage) {
						target = ((SetTargetMessage)inMessage).getTarget();
						if (job_id != null) {
							new Thread(()->{resetMining();}).start();
						}
					}
					if (inMessage instanceof NotifyMessage) {
						NotifyMessage nMessage = (NotifyMessage)inMessage;
//						if (!nMessage.isClean_jobs()) { TODO
//							throw new IllegalStateException("Got 'clean jobs' : false");
//						}
						job_id = nMessage.getJob_id();
						nversion = nMessage.getNversion();
						hash_prev_block = nMessage.getHash_prev_block();
						hash_merkle_root = nMessage.getHash_merkle_root();
						hash_reserved = nMessage.getHash_reserved();
						ntime = nMessage.getNtime();
						nbits = nMessage.getNbits();
						if (target != null) {
							new Thread(()->{resetMining();}).start();
						}
					}
				}
			} catch (IOException | ParseException e) {
				logger.error("Error reading incoming stratum messages: " + e);
				System.exit(-121); //TODO ?
			}
		}).start();
	}
	
	public void startMining() throws IOException {
		openConnection();
		startConversationThread();
		sendMessage(new SubscribeMessage(creds.host, creds.port));
		sendMessage(new AuthoriseMessage(creds.username, creds.password));
	}
	
	private void sendMessage(OutMessage message) {
		if (message.getId() != 0) {
			if (message instanceof SubscribeMessage) {
				expected.put(message.getId(), new SubscribeRsMessage());
			} else {
				expected.put(message.getId(), new InMessage());
			}
		}
		logger.debug("-->>--:" + message);
		pw.println(message);
		pw.flush();
	}
	
	
	
}
