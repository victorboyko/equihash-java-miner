package victor.miner.equihash;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public abstract class Solver {

	
	/**
	 * 
	 * Receives parameters in hex format (the same length to be used in stratum)
	 * 
	 * @return Solutions in hex including 3 bytes prefix (ready to be used in stratum submit)
	 */
	public List<String> solve(
			String version, 
			String prevhash, 
			String merkleRoot, 
			String reserved, 
			String ntime, 
			String nbits, 
			String nonceLeft, 
			String nonceRight) {
		
		String workStr = version + prevhash + merkleRoot + reserved + ntime + nbits + nonceLeft + nonceRight;		
		byte[] work = new BigInteger(workStr, 16).toByteArray(); // not sure this is safe and work always properly
		
		List<byte[]> solutions = solve(work);
		List<String> result = new ArrayList<>(solutions.size());
		for(byte[] solution : solutions) {
			String sol = new BigInteger(1, solution).toString(16);
			sol = "00000000000000000000000".substring(0, 1344*2-sol.length()) + sol;
			result.add("fd4005" + sol);
		}
		return result;
	}
	
	/**
	 * Single or default amount of threads to be used
	 * @param work
	 * @return
	 */
	public abstract List<byte[]> solve(byte[] work);
	
	/**
	 * 
	 * @param work
	 * @return
	 */
	public abstract List<byte[]> solve(byte[] work, int threadsNum);
	
}
