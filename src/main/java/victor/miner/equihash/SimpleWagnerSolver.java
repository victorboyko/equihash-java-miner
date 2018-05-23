package victor.miner.equihash;

import static victor.miner.equihash.EquihashUtil.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.net.SyslogAppender;

import ove.crypto.digest.Blake2b.Digest;

public class SimpleWagnerSolver extends Solver {
	
	abstract static class HashNode implements Comparable<HashNode>{
		private byte[] hash;
		
		public abstract int[] getIndexes();
		
		public HashNode(byte[] hash) {
			this.hash = hash;
		}
		
		public byte[] getHash() {
			return hash;
		}
		
		@Override
		public int compareTo(HashNode o) {
			for(int i = 0; i < hash.length ; i++) {
				if (hash[i] != o.hash[i]) {
					return hash[i] - o.hash[i];
				}
			}
			return 0;
		}
		
		public void dropHash() {
			hash = null;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for(byte b : hash) {
				String binStr = Integer.toBinaryString((b+256)%256);
				sb.append("00000000".substring(0, 8-binStr.length()));
				sb.append(binStr);
			}
			return sb.toString();
		}
	}
	
	static class HashLeaf extends HashNode {
		
		public HashLeaf(byte[] hash, int index) {
			super(hash);
			this.index = index;
		}
		
		private int index;
		
		@Override
		public int[] getIndexes() {
			return new int[] { index };
		}
	}
	
	static class HashMidNode extends HashNode {
		private HashNode left, right;
		
		public HashMidNode(byte[] hash, HashNode left, HashNode right) {
			super(hash);
			this.left = left;
			this.right = right;
		}
		
		@Override
		public int[] getIndexes() {
			int[] leftInds = left.getIndexes();
			int[] rightInds = right.getIndexes();
			int[] result = ArrayUtils.addAll(leftInds, rightInds);
			return result;
		}
	}
	
	@Override
	public List<byte[]> solve(byte[] work) {
		return solve(work, 32);
	}
	
	@Override
	public List<byte[]> solve(byte[] work, int tNum) {

		List<byte[]> result = new ArrayList<>();
		
		long start = System.currentTimeMillis();
		
		
		final List<HashNode> hashesSynch = Collections.synchronizedList(new ArrayList<>());		
		List<HashNode> hashes = hashesSynch;
		
		final int hNum = 1048576; // 2^20 initially
		final CountDownLatch latch = new CountDownLatch(tNum);
		
		Function<Integer, Void> f = (c) -> {
			Digest d = getEquihash200_9PowDigest();
			for(int i = c * ((hNum + tNum - 1) / tNum); i < Math.min((c+1) * ((hNum + tNum - 1) / tNum), hNum ); i++) {
				d.update(work);
				byte[] hash = d.digest(intToLEByteArray(i));
				hashesSynch.add(new HashLeaf(Arrays.copyOfRange(hash, 0, 25), 2*i));
				hashesSynch.add(new HashLeaf(Arrays.copyOfRange(hash, 25, 50), 2*i+1));

			}
			latch.countDown();
			return null;
		};
		

		for(int i = 0; i < tNum; i++) {
			final int j = i;
			new Thread(()-> {f.apply(j);}).start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		new BucketSort(12).sort(hashes, tNum);			
//		Collections.sort(hashes);
		hashes = findMatches(hashes, tNum);
		
		for(HashNode node : hashes) {
		
			int[] indexes = node.getIndexes();
			Set<Integer> indexesSet = Arrays.stream(indexes).boxed().collect(Collectors.toSet());
					
			if (indexesSet.size() < 512) {
				continue; // contains duplicate indexes
			}
			
			//Arrays.sort(indexes);  // TODO, to check how the indexes order should be defined
			byte[] compressed = EquihashUtil.compressSolutionTo21BitIndexes(indexes);
			
			result.add(compressed);
		}
		
//		long end = System.currentTimeMillis();
//		System.out.printf("It took %2.5f seconds having %d solutions found\n" , (end - start)/1000d, result.size());
		
		return result;
	}
	
	static List<HashNode> findMatches(List<HashNode> hashes, int tNum) {
		List<HashNode> result = Collections.synchronizedList(new ArrayList<>());
		
		final CountDownLatch latch = new CountDownLatch(tNum);
		
		Function<List<HashNode>, Void> f = (hashesPart) -> {
			result.addAll(findMatchesOneThread(hashesPart));
			latch.countDown();
			return null;
		};
		
		int portionSize = (hashes.size()+tNum-1)/tNum;
		for(int i = 0; i < tNum; i++) {
			final List<HashNode> hashesPart = new ArrayList<>(hashes.subList(i*portionSize, Math.min(1 + (i+1)*portionSize, hashes.size())));
			new Thread(()-> {f.apply(hashesPart);}).start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return result;
	}
	
	private static List<HashNode> findMatchesOneThread(List<HashNode> hashes) {
		for(int step = 0; step < 9; step++) {
			if (hashes.size() == 0) {
				break;
			}
			
			
			if (step >= 3) {
				System.out.print(" [" + step + "] : ");
				List<HashNode> sublist = hashes.subList(0, Math.min(10, hashes.size()));
				System.out.println(hashes.size() + " " + Arrays.asList(sublist));
//				for(HashNode node : sublist) {
//					System.out.println(" >> " + node.toString().substring(20*step));
//				}
			}
			
			HashNode current = hashes.get(0);
	
			List<HashNode> matched= new ArrayList<>();
			List<HashNode> newHashes = new ArrayList<>();
			matched.add(current);
			for(int c = 1; c < hashes.size()+1; c++){
				
//				if (c % 20000 == 0) {
//					System.out.print(".");
//				}
				
				boolean gotMatched = false;
				HashNode node = null;
				if (c != hashes.size()) {
					node = hashes.get(c); 
					if (step >= 4) {
						System.out.println("here"); //TODO
					}
					if (node.getHash().equals(current.getHash())) {
						continue;
					}
					gotMatched = matchBits(current.getHash(), node.getHash(), 20*step, 20);
				} 
				
				if(gotMatched) {
					matched.add(node);
				} else {
					
					if (matched.size() == 1) {
//						hashes.remove(--c); // NOT in arraylist!
					} else {						
						
						for(int i = 0; i < matched.size(); i++) {
							HashNode a = matched.get(i);
							for(int j = i+1; j < matched.size(); j++) {
								HashNode b = matched.get(j);
								byte[] xored = new byte[25];
								for(int k = 0; k < xored.length; k++) {
									xored[k] = (byte)(a.getHash()[k] ^ b.getHash()[k]);
								}
								// I don't check same ids to be used in different nodes, will be checked at the last stage
								HashNode newNode = new HashMidNode(xored, a, b);
								newHashes.add(newNode);
							}
							a.dropHash();
						}						
					}
					matched.clear();
					current = node;
					matched.add(current);
				}
				
			}
			
			hashes = newHashes;
			
			new BucketSort(12).sort(hashes, 1);			
//			Collections.sort(hashes);
			
//			System.out.println();
		}
		return hashes;
	}
	
	public static void main(String[] args) {
		String version = "04000000";
		String prevhash = "ddc0c6dd1a3395b47769afb5bd3558451b95ce046b13886982e56f0300000000";
		String merkleRoot = "8a7e3a9b27a4644084990459e79a5d48cbac008e0e4f186fcbba49e2324d4609";
		String reserved = "0000000000000000000000000000000000000000000000000000000000000000";
		String ntime = "168a955a";
		String nbits = "8bf10e1c";
		String nonceLeft = "003ccef0d7";
		String nonceRight = "c80000000000000000000000000000000000000000000000000001";
		
		List<String> solutions;
		
		do { 
			String newNonce = new BigInteger(300, new Random()).toString(16) + "000000000000000000000000000000000000000000000000000000";
			nonceRight = newNonce.substring(0, nonceRight.length());
			System.out.println(nonceRight);
		} while((solutions = new SimpleWagnerSolver().solve(version, prevhash, merkleRoot, reserved, ntime, nbits, nonceLeft, nonceRight)).size() == 0);
		System.out.println("Solved!\n" + solutions);
		System.out.println();
	}

}
