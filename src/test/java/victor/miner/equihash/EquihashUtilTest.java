package victor.miner.equihash;

import static org.junit.Assert.*;

import org.junit.Test;

public class EquihashUtilTest {

	@Test
	public void testMatchBitsFromZeroOffset() {
		assertFalse(EquihashUtil.matchBits(new byte[] {0, 0, 0b00110011}, new byte[] {0, 0, 0}, 0, 20));
	}

}
