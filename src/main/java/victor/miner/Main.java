package victor.miner;

import java.io.IOException;

import org.apache.log4j.Logger;

import sun.awt.Symbol;
import victor.miner.stratum.StratumClient;

public class Main {
	
	private static final Logger logger = Logger.getLogger(Main.class);
	
	private static void showHelpMessage() {
		System.out.println("+-------------------------------------------------+");
		System.out.println("|              Victor's miner     v0.1            |");
		System.out.println("+-------------------------------------------------+");
		System.out.println(PoolCredentials.helpMessage);
	}
	
	
	public static void main(String[] args) {
		
		//String argsStr = "--server zel.cloudpools.net --user t1TFwDdJ9LWF3wMw8rh4KmN2Hkrcr71ymcF.javaRig1 --pass x --port 3052";
		//String argsStr = "--server eu1-zcash.flypool.org --user t1VL1tTafDkVnvkiS4R84Mey5WWYKcgMpKf.javaRig1 --pass x --port 3333";
		String argsStr = "--server zen.suprnova.cc --user log121.mainRig1 --pass d=4 --port 3618";
		args = argsStr.split(" ");
		
		PoolCredentials poolCreds = PoolCredentials.getFromArgs(args);

		if (poolCreds == null) {
			showHelpMessage();
			System.exit(0);
		}
		
		StratumClient client = new StratumClient(poolCreds);
		try {
			client.startMining();
		} catch (IOException e) {
			logger.error("Error from stratum client: " + e);
		}

	}
	
}
