package victor.miner.equihash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import victor.miner.equihash.SimpleWagnerSolver.HashNode;

public class BucketSort {

	private final int K;
	private final int N;
	private final int mask;
	private List<HashNode>[] buckets; 
	
	public BucketSort(int k) {
		this.K = k;
		N = 2 << k;
		buckets = new List[N];
		mask = N - 1;
	}
	
	private int toPositiveInt(byte b) {
		return (256 + b) % 256;
	}
	
	public void sort(List<HashNode> list, int tNum) {
		sort(list, tNum, Comparator.naturalOrder());
	}
		
	
	public void sort(List<HashNode> list, int tNum, Comparator<HashNode> cmp) {
		for(int i = 0; i < N; i++) {
			buckets[i] = new ArrayList<HashNode>();
		}
		for(HashNode node : list) {
			int index = toPositiveInt(node.getHash()[0]) + (toPositiveInt(node.getHash()[1]) << 8);
			index &= mask;
			buckets[index].add(node);
		}
		final CountDownLatch latch = new CountDownLatch(tNum);
		final int portion = (N + tNum - 1) / tNum;
		Function<Integer, Void> f = (c) -> {
			int startInd = c * portion;// inclusive
			int endInd = Math.min((c + 1) * portion, N); // exclusive
			for(int i = startInd; i <  endInd; i++) {
				Collections.sort(buckets[i], cmp);
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
		list.clear();
		for(int i = 0; i < N; i++) {
			Collections.sort(buckets[i], cmp);
			list.addAll(buckets[i]);
		}
	}
	
}
