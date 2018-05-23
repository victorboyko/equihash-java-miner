package victor.miner.equihash;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import ove.crypto.digest.Blake2b.Digest;
import ove.crypto.digest.Blake2b.Param;

public class EquihashUtil {
	
	public static int get20bits(byte[] arr, boolean fourBitsRight) {
		int result = (((arr[0]+256)%256)<<16) + (((arr[1]+256)%256)<<8) + ((arr[2]+256)%256);
		result &= (fourBitsRight) ? 0x0FFFFF : 0xFFFFF0;
		if (!fourBitsRight) result >>= 4;
		return result;
	}
	
	public static byte[] xorExcept1stNbytes(byte[] arr1, byte[] arr2, int nBytes) {
		byte[] result = new byte[arr1.length-nBytes];
		for(int i = 0; i < result.length; i++) {
			result[i] = (byte)(arr1[i+nBytes] ^ arr2[i+nBytes]);
		}
		return result;
	}
	
	public static String byteArrayToHexString(byte[] hash) {
		String hashStr = new BigInteger(1, hash).toString(16);
		hashStr = "00000000000000000000000".substring(0, hash.length*2-hashStr.length()) + hashStr;
		return hashStr;
	}
	
	public static boolean fitsTarget(String work, String solutionFull, String target) {
		byte[] blockCandidate = new BigInteger(work + solutionFull, 16).toByteArray();
		String hash = DigestUtils.sha256Hex(DigestUtils.sha256(blockCandidate));
		char[] chars = new char[hash.length()]; // inverting hex pairs(bytes), to treat as little endian
		for(int i = 0; i < chars.length/2; i++) {
			chars[i*2] = hash.charAt(chars.length-1-i*2-1);
			chars[i*2+1] = hash.charAt(chars.length-1-i*2);
		}
		hash = new String(chars);
		return hash.compareTo(target) < 0;
	}
	
	public static List<String> getSharesFromSolutions(List<String> solutions, String work, String target) {
		List<String> shares = new ArrayList<>();
		for(String sol : solutions) {
			if (EquihashUtil.fitsTarget(work, sol, target)) {
				shares.add(sol);
			}
		}
		return shares;
	}

	public static Digest getEquihash200_9PowDigest() {
		Param param = new Param();
		param.setDigestLength(50);
		
		byte[] personalization = new byte[16];
		System.arraycopy("ZcashPoW".getBytes(), 0, personalization, 0, 8);
		personalization[8]	= -56; // 200 in little endian
		personalization[12] = 9;
		param.setPersonal(personalization);
				
		Digest d = Digest.newInstance(param);
		return d;
	}
	
	public static byte[] intToLEByteArray(int data) {
		byte[] result = new byte[4];
		result[3] = (byte) ((data & 0xFF000000) >> 24);
		result[2] = (byte) ((data & 0x00FF0000) >> 16);
		result[1] = (byte) ((data & 0x0000FF00) >> 8);
		result[0] = (byte) ((data & 0x000000FF) >> 0);
		return result;
	}
	
	public static byte[] compressSolutionTo21BitIndexes(int[] indexes) {
		//byte[] result = new byte[512*21/8]; //1344 elements

		BigInteger accum = BigInteger.ZERO;
		for(int i = 0; i < 512; i++) {
			accum = accum.shiftLeft(21);
			accum = accum.add(BigInteger.valueOf(indexes[i]));
		}
		byte[] result = accum.toByteArray();
		if (result.length < 512*21/8) {
			byte[] extended = new byte[512*21/8];
			System.arraycopy(result, 0, extended, extended.length - result.length, result.length);
			return extended;
		}
		return result;
	}
	
	public static boolean matchBits(byte[] a, byte[] b, int offset, int bitsNum) {
		int begin = offset / 8; // inclusive
		int end = (offset + bitsNum + 7) / 8; //exclusive
		for(int i = begin; i < end; i++) {
			byte c = (byte)(a[i]^b[i]);
			if (i == begin) {
				int mask = (1 << (8 - offset % 8)) - 1;
				c &= mask;
			}
			if (i == end-1) {
				int mask = (1 << (8- (offset + bitsNum) % 8)) - 1;
				c &= mask;
			}
			if (c != 0) { 
				return false;
			}
		}
		return true;
	}
}
