package victor.miner;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

import ove.crypto.digest.Blake2b.Digest;

public class Blake2bTest {

	@Test
	public void test() {		
		String message = "The quick brown fox jumps over the lazy dog"; // taken from here: https://en.wikipedia.org/wiki/BLAKE_(hash_function)
		String exResult 	= "A8ADD4BDDDFD93E4877D2746E62817B116364A1FA7BC148D95090BC7333B3673"
							+ "F82401CF7AA2E4CB1ECD90296E3F14CB5413F8ED77BE73045B13914CDCD6A918";
		
		byte[] acResult = Digest.newInstance().digest(message.getBytes());
		BigInteger bi = new BigInteger(1, acResult);
		assertEquals(bi.toString(16).toUpperCase(), exResult);
	}

}
