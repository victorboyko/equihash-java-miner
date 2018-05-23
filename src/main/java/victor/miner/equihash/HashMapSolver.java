package victor.miner.equihash;

import static victor.miner.equihash.EquihashUtil.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import org.apache.commons.lang.ArrayUtils;

import ove.crypto.digest.Blake2b.Digest;

public class HashMapSolver extends Solver implements Cancelable {
	
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
			return byteArrayToHexString(hash);
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
		return solve(work, 16);
	}

	@Override
	public List<byte[]> solve(byte[] work, int threadsNum) {
		List<byte[]> result = new ArrayList<>();
		long start = System.currentTimeMillis();
		final int hNum = 1048576; // 2^20 initially
		Map<Integer, Object> hashes = Collections.synchronizedMap(new HashMap<Integer, Object>());
		
//		Set<Integer> tracked = new HashSet<>();
//		for(int t : new int[] {674126, 1254551, 78809, 1165243, 1339935, 1455771, 742821, 1200381}) {
//			tracked.add(t);
//		}
		
		

		for(int i = 0; i < hNum; i++) {
			Digest d = getEquihash200_9PowDigest();
			d.update(work);
			byte[] hash = d.digest(intToLEByteArray(i));
			HashNode node1 = new HashLeaf(Arrays.copyOfRange(hash, 0, 25), 2*i);
			HashNode node2 = new HashLeaf(Arrays.copyOfRange(hash, 25, 50), 2*i+1);
			for(HashNode node : new HashNode[] {node1, node2}) {

//				if (tracked.contains(node.getIndexes()[0])) {
//					System.out.println(new BigInteger(node.getHash()).toString(16));
//				}
				
				final int bits = get20bits(node.getHash(), false);
				if (!hashes.containsKey(bits)) {
					hashes.put(bits, node);
				} else {
					Object obj = hashes.get(bits);
					if (obj instanceof HashNode) {
						List<HashNode> collide = new ArrayList<>(2);
						collide.add((HashNode)obj);
						collide.add(node);
						hashes.put(bits, collide);
					} else {
						((List<HashNode>)obj).add(node);
					}
				}
			}
		}
		
		
		for(int i = 1; i < 10; i++) {
			if (isCancelled()) return result;
			
			System.out.println(i + " : " + hashes.size());
			final Map<Integer, Object> newHashes = Collections.synchronizedMap(new HashMap<Integer, Object>());
			
			final int i2 = i;
			final CountDownLatch latch = new CountDownLatch(threadsNum);
			

			
			Function<List<Integer>, Void> f = (c) -> {

				for(int key : c) {
					Object objCollide = hashes.get(key);
				
					if (objCollide == null || isCancelled()) {
						break;
					}
					if (!(objCollide instanceof List)) {
						((HashNode)objCollide).dropHash();
						continue;
					}
					List<HashNode> collided = (List<HashNode>)objCollide;
					for(int j = 0; j < collided.size() - 1; j++) {
						out1:
						for(int k = j+1; k < collided.size(); k++) {
							if (j == k) {
								continue;
							}
							HashNode node1 = collided.get(j);
							HashNode node2 = collided.get(k);
	//							if (i > 1 && (  ((HashMidNode)node1).left == ((HashMidNode)node2).left ||
	//											((HashMidNode)node1).left == ((HashMidNode)node2).right ||
	//											((HashMidNode)node1).right == ((HashMidNode)node2).left ||
	//											((HashMidNode)node1).right == ((HashMidNode)node2).right   )) {
	//								continue;
	//							}
							int[] inds1 = node1.getIndexes();
							int[] inds2 = node2.getIndexes();
							
							if (inds1[0] > inds2[0]) {
								int[] temp = inds1;
								inds1 = inds2;
								inds2 = temp;
								HashNode tempNode = node1;
								node1 = node2;
								node2 = tempNode;
							}
							
							int[] allInds = ArrayUtils.addAll(inds1, inds2);
							
							Set<Integer> indices = new HashSet<>();
							for(int z : allInds) {
								if (indices.contains(z)) {
									continue out1;
								}
								indices.add(z);
							}
							
							if (node1.getHash() == null || node2.getHash() == null) {
								System.out.println("here");//TODO
							}
							
							HashNode node = new HashMidNode(xorExcept1stNbytes(node1.getHash(), node2.getHash(), 2 + ((i2+1) % 2)), node1, node2);
							
							final int bits = get20bits(node.getHash(), i2 % 2 == 1);
							if (i2 == 9 && bits != 0) {
								continue;
							}
							
							if (!newHashes.containsKey(bits)) {
								newHashes.put(bits, node);
							} else {
								Object obj = newHashes.get(bits);
								if (obj instanceof HashNode) {
									List<HashNode> collide = Collections.synchronizedList(new ArrayList<>(2));
									collide.add((HashNode)obj);
									collide.add(node);
									newHashes.put(bits, collide);
								} else {
									((List<HashNode>)obj).add(node);
								}
							}
						}					

					}
					
					for(HashNode node : collided) {
						node.dropHash();
					}
					
				}
				latch.countDown();
				return null;
			};

			final List<Integer> keys = new ArrayList<>(hashes.keySet());
			
			int portion = (keys.size()+threadsNum-1)/threadsNum;
			for(int t = 0; t < threadsNum; t++) {
				List<Integer> partOfKeys = keys.subList(t*portion, Math.min((t+1)*portion, keys.size()-1));
				//final int j = t;
				new Thread(()-> {f.apply(partOfKeys);}).start();
			}
			try {
				latch.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			
			hashes.clear();
			hashes.putAll(newHashes);
//			hashes = newHashes;
			System.gc();
		}
		
		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream("out.txt", false);
			pw = new PrintWriter(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		/*
		class EQArray {
			int[] data;
			public EQArray(int[] arr) {
				data = arr;
			}
			@Override
			public boolean equals(Object obj) {
				if (obj == this) return true;
				if (!(obj instanceof EQArray)) return false;
				EQArray o2 = (EQArray)obj;
				return Arrays.equals(data, o2.data);
			}
			@Override
			public int hashCode() {
				return data[5]+data[27]<<21;
			}
		}
		
		Set<EQArray> bank = new HashSet<>();
		*/
		Set<String> uniqueSols = new HashSet<>();
		
		System.out.println(10 + " : " + hashes.size());
		int cnt = 0;
		out2:
		for(Object objCollide : hashes.values()) {
			
			if (cnt++ % 10000 == 0) {
				System.out.printf("%.0f%% complete\n",  (100d * cnt/(hashes.size())));
			}
			
			for(HashNode node : (objCollide instanceof List) ? ((List<HashNode>)objCollide) : Arrays.asList((HashNode)objCollide) ) {
							
				byte[] solArray = compressSolutionTo21BitIndexes(node.getIndexes());
				String sol = byteArrayToHexString(solArray);
				
				if (!uniqueSols.contains(sol)) {
					uniqueSols.add(sol);
					result.add(solArray);
					pw.println(sol);
					if (result.size() >= 30) break out2; // ! 30 is enough TODO						
				}				
				
			}
			
		}
		System.out.println("100% complete");
		
		pw.flush();
		pw.close();
		
		long end = System.currentTimeMillis();
		System.out.printf("It took %2.5f seconds having %d solutions found\n" , (end - start)/1000d, result.size());
		return result;
	}
	
	private boolean cancelled; // = false;
	private synchronized boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public synchronized void cancel() {
		cancelled = true;
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
		Solver solver = new HashMapSolver();
		
		do { 
			String newNonce = new BigInteger(300, new Random()).toString(16) + "000000000000000000000000000000000000000000000000000000";
			nonceRight = newNonce.substring(0, nonceRight.length());
			System.out.println(nonceRight);
		} while((solutions = solver.solve(version, prevhash, merkleRoot, reserved, ntime, nbits, nonceLeft, nonceRight)).size() == 0);
		System.out.println("Sols : " + solutions.size());
		String nonce = nonceLeft + nonceRight;
		String work = version + prevhash + merkleRoot + reserved + ntime + nbits + nonce;
		List<String> shares = new ArrayList<>();
		for(String sol : solutions) {
			if (fitsTarget(work, sol, "0004189374bc6a7ef9db22d0e5604189374bc6a7ef9db22d0e5604189374bc6a")) {
				shares.add(sol);
			}
		}
		System.out.println("Shares : " + shares.size());
	}
}
