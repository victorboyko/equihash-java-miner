package victor.miner;

import static org.junit.Assert.*;

import org.junit.Test;

public class PoolCredentialsTest {


	@Test
	public void testPositiveScenario() {
		String commandArgs = "--user victor --server proxypool.info --pass x --port 7721";
		String[] args = commandArgs.split(" ");
		PoolCredentials creds = PoolCredentials.getFromArgs(args);
		assertEquals(creds.host, "proxypool.info");
		assertEquals(creds.port, 7721);
		assertEquals(creds.username, "victor");
		assertEquals(creds.password, "x");
	}

	@Test
	public void testMissedParameter() {
		String commandArgs = "--user victor --server proxypool.info --port 7721";
		String[] args = commandArgs.split(" ");
		PoolCredentials creds = PoolCredentials.getFromArgs(args);
		assertNull(creds);
	}
	
	@Test
	public void testMissedParameterValue() {
		String commandArgs = "--user victor --server proxypool.info --port 7721 --pass";
		String[] args = commandArgs.split(" ");
		PoolCredentials creds = PoolCredentials.getFromArgs(args);
		assertNull(creds);
	}
	
	@Test
	public void testMalformedRequest() {
		String commandArgs = "--user --port 7721 victor --server proxypool.info --pass x";
		String[] args = commandArgs.split(" ");
		PoolCredentials creds = PoolCredentials.getFromArgs(args);
		assertNull(creds);
	}
}
