package victor.miner.equihash;

import static victor.miner.equihash.EquihashUtil.*;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.log4j.Logger;

import ove.crypto.digest.Blake2b.Digest;
import ove.crypto.digest.Blake2b.Param;


public class Verifier {
	private static final Logger logger = Logger.getLogger(Verifier.class);
	
//	private int n, k;
//	
//	public Verifier(int n, int k) {
//		this.n = n;
//		this.k = k;
//	}
	
	/**
	 * @param work - 140b block header
	 * @param sol - solution excluding 1st 3 bytes (length)
	 */
	public static boolean verify(byte[] work, byte[] solution) {
		BigInteger result = BigInteger.ZERO;
				

		
		for(int i = 0; i < 512; i++) {
			int iInd = i * 21 / 8; // starting byte index
			int iBitInd = (iInd+4) * 8 % 21; // num bits to shift right after we read 4 bytes
			
			byte[] iBytes = new byte[4];
			System.arraycopy(solution, iInd, iBytes, 0, Math.min(4, solution.length - iInd));
			long iValLong = 0;
			for(int j = 0; j < 4; j++) {
				iValLong <<= 8;
				int bVal = iBytes[j] >= 0 ? iBytes[j] : 256 + iBytes[j];
				iValLong += bVal;
			}
			int iVal =(int)((iValLong >> iBitInd) & 0x1FFFFF);
			//logger.debug(Integer.toBinaryString(iVal) + " : " + iVal);
			
			Digest d = getEquihash200_9PowDigest();
			d.update(work, 0, 140);
			d.update(intToLEByteArray(iVal / 2));
			byte[] hash = d.digest();
			byte[] halfHash = Arrays.copyOfRange(hash, 25*(iVal % 2), 25*(iVal % 2 + 1));
			logger.debug(byteArrayToHexString(halfHash));
			result = result.xor(new BigInteger(1, halfHash));
		}
		
		logger.debug("XOR result: " + result.toString(16));
		return result.equals(BigInteger.ZERO);
	}
	
	public static boolean verify(String workHex140b, String solHex1347b) {
		
		if (!solHex1347b.toLowerCase().startsWith("fd4005") || solHex1347b.length() != 1347*2) {
			logger.error("solution length is incorrect");
			return false;
		}
		
		if (workHex140b.length() != 140*2) {
			logger.error("Worker length is incorrect: " + workHex140b.length()/2 + " (expected 140)");
			return false;
		}
		
		byte[] work = new BigInteger(workHex140b, 16).toByteArray();
		
		byte[] solutionNoPrefix = new BigInteger(solHex1347b.substring(6), 16).toByteArray();
		byte[] solution = new byte[1344];
		if (solutionNoPrefix.length != solution.length) {
			System.arraycopy(solutionNoPrefix, 0, solution, solution.length-solutionNoPrefix.length, solutionNoPrefix.length);
		} else {
			solution = solutionNoPrefix;
		}
		
		return verify(work, solution);
	}
	
	public static void main(String[] args) {
		String version = "04000000";
		String prevhash = "ddc0c6dd1a3395b47769afb5bd3558451b95ce046b13886982e56f0300000000";
		String merkleRoot = "8a7e3a9b27a4644084990459e79a5d48cbac008e0e4f186fcbba49e2324d4609";
		String reserved = "0000000000000000000000000000000000000000000000000000000000000000";
		String ntime = "168a955a";
		String nbits = "8bf10e1c";
		String nonceLeft = "003ccef0d7";
		//String nonceRight = "c80000000000000000000000000000000000000000000000000001";
		String nonceRight = "7f4d6816d89c8e03df5a6518259043b95e215b31b48a0269980631";
		String nonce = nonceLeft + nonceRight;
		
		String work = version + prevhash + merkleRoot + reserved + ntime + nbits + nonce;
		//String solution = "fd400501273d950e9a4a6b7aea40383bcdf7fdbd525a3110160d52e9a518f34177a25191ef9b301ab3ea7a8f0910372a51ee516a79fe5466fb83f6bc77157fbf7e42278a0bc5f3619a276ec675fc3bfb754a36611eee0a0c7a43f3a8840fbfeb1e356fd5b20c29a32871d6c3357a2a45f4d9f913292f754407c32ee1b3255048af15422fab41c962ef93a5616f5d35b1869dfdde20e41599d9d13a97b0a4ea03624a13b7042cd455eb0ce3013692ecbe4a22a2ca6b094494e56c0f8db11f82606c782ea72beee04ddb8c18e0c3c77b96ca6139ec1511911213201b2ad532f1e8c9867f7a4aff603b26ee1bf84c8f72cd3d84c56122129365fe9d58471d82ba1505824b6cd556f9fb96348f00fdfea1616f4eb9e35123e5209130ee33af0758240a7fc743ae423e8e761cef5dd8ae14d46d874193d2de280813b984dfad8a2724db8e589353d8fc66671e5873494e5676fdfcd504426beb89879818a97a03f9bf3ee68dcbb09edc30199077832487eee379b71422aedb93adff3efd1362050a2663c26fa4f9fb63a61e3ad2546dcff0f1abb62c12c174cee97d535e0906240358b1adacc40eb61a09b37447f5448ace359221f16bfbbac10be737760537af353915f540e1cfe695fda27b5397a660de7c460a84d662296361e1798120b86be9f1daf6c679828133db6f142090c3f4891917ad3156f61a304b7ca340073082201c127b42dfefb278f2b30319e4ddf40de00804e9d05bcf3a1ce7c9e36078c4ffe9274b5637360fff54826f4e02e2eac7e332c2337a7ecf26b7ea001e7e9f5ea9151f42bc7b333032a5ed2f334d7c7f45098b1ec8d8524890a085a3888a41b780fa8a5894930e0d3cb45e22c2935784a678d6405059c20571e09b0a06fd16b058de3b1f830115751bf3a1110c51d95024239c30a057d593ce8f25c1ef477d1ae8493b5200034fa053c416d1914381862213664cc62e4eda1b6f106a2328aa624b69a230c41ee3b86a21bae2722c4e19cd7414bc187cdd64e92301dbdaa850e89ba7ddc050758cc88f57ba930ccee69ccad3bf6f32d8bfb62b04c127a05c18fe9139bb60706105169e169215f6d91a512cb6bc8b200cea6509a68d59ae62e3f1bed7e7058401894bc7a50cff44e34bdcb4b895446a33a7c43f80df1a56e1977f5fae47b478f6feca2d0f99ba43068ecb804212a993a397c0e25f524a543b37736d5f101421445ff270edd7efab4e6fe8a2876bb0bcfeb40c6682ab6a99d9b969cc16f565bd7d3b188d9bddbb36804e4c32908d6fdfc58ce4c17cab6b84c6fdb774087eeb062892013d0eae45098d66a44f7ec05e71ca0ec1d4aef5d6ac97d0af85b791ffd9be765177ebac10101e01de60d50142e8d2f050ed15b5821f158bc237c74a1ea368d4bd5e2c670b93cd5bb24ffd59ad3204ce3351cb21e9f75a76811d31c1daf1140a9a85890b2cdff9c060678beca4a232e02c6b40bba03456852207a1b75289db38d99f9361c64bfbad178217066228df9c881f5ddb18fa16c46fb14f84720e6e10c5230b53ee95f74f3b3c8183f1dc77bbfa3dd91679f40e33d64e10565a3a33b64715eefa64a481989954f1e02d422375ea50ad695d89454ad44e58497cf33152dc2dbc64658258efd2dd5d8642a6e1ba59ef739a2ff70802a48f9b47cd12a657f0bf48bfdd358cd6af5b0414940c3d90460bcd562998a54a76be6f65d6dbef8d1dd9a53e5933bb0dba9293bd80dc267a072a7235822f7fd4e28a1d66dfcee7a65e5efc825e54cadd8e0b18472369695381d2e2c4f9a288d78b2a918abd0d223b224e744e2c8bd5efd7686e22f2c3639dd13e907519349c32a60bacc7837d31eaca3a0df604019c961736bbdd5ddd6bc727edf8d48ec2e89019e8681f8940";
		String solution = "fd40050148f2121debbf3d872ec0848cbb6c5511468fb3e104a41df90628b717e4ae0115ee3b19719bb4d2729311ca62844b0ea0a782200cfbe76bee53aad31fde461864cc0783871a49d0c635cbd8c5831e16359305210844ff9b50083cb331e406c33db69c5a51a81b673e12f9c7ae2fc88159b9b025fb625c3581a617b35f26189da6e3950e9922fc5df2b948d137725b7ad67b531dcc8e0e2cd8acdfd79a4785c8fc89ca1cfebd5cda040a6f33d1c67b9662a3c47920db3f1e86963e10ac0e3af8c5424e53b1d4e9e6e7dd5a02c2e79f37679220360d0418a44e89b232f5c9ba2f445dd5effb0fe9332fcb8d6c6b7113807b15a5b3ff1c6a008cffb5a505fdb83cf6a3e51d275443d8529f919d3e90cffd8711c18ef85333854dbce15156aa3f07865d9a3af3f008a6155dcc2de581c7ff3476a47a88197a7d9349af17c50ddb9a9418b39a0214e513dda99398e11f388202a9d91f64a40317298560f6a222c1dcf764e9421307efcc3d14039785057bb211b65380129d911ca8f905ce2ba043c96d8aed87e0eefde56695ab35342c871e1c5e36541180f77a968280a1c33d8dc028381cd2067e8a1d4643b4bffe32c24fdbaffd5eae0858524b0dac669c00c5f062d165119d3dcf8e22ca94fe482b06a0fe7d0babfb477b7ad62540e99d21d18df549e647f6c721ae596fa95e6d5544d2ad709263a39913e90604a0423d0a18a8bd43e5d0c338882e637cdb5b45521cf4dc6bda14d9688b56db2db71515be2c8e8f140677a1f04b9180430a9db124cbd22a8e9cecba044c3ac0a20ef019f9bdaf69a7175eeef2af57819c1f9b070ac411d9cbb0dead392305a3a7a6112d825986d359586504c0ea9ca97c2feabb5f62e102e39bf9d9ed22ed6eed8f9e47f5482384cf31bbc4aa39fedda9042dfb926fdd20f2fd9ed8a50dedbd91c67cb9dcddc00148f2121debbf3d872ec0848cbb6c5511468fb3e104a41df90628b717e4ae0115ee3b19719bb4d2729311ca62844b0ea0a782200cfbe76bee53aad31fde461864cc0783871a49d0c635cbd8c5831e16359305210844ff9b50083cb331e406c33db69c5a51a81b673e12f9c7ae2fc88159b9b025fb625c3581a617b35f26189da6e3950e9922fc5df2b948d137725b7ad67b531dcc8e0e2cd8acdfd79a4785c8fc89ca1cfebd5cda02a9d91f64a40317298560f6a222c1dcf764e9421307efcc3d14039785057bb211b65380129d911ca8f905ce2ba043c96d8aed87e0eefde56695ab35342c871e1c5e36541180f77a968280a1c33d8dc028381cd2067e8a1d4643b4bffe32c24fdbaffd5eae0858524b0dac669c00c5f062d165119d3dcf8e22ca94fe482b06a0fe7d0babfb477b7ad62540e99d21d18df549e647f6c721ae596fa95e6d5544d2ad709263a39913e9040a6f33d1c67b9662a3c47920db3f1e86963e10ac0e3af8c5424e53b1d4e9e6e7dd5a02c2e79f37679220360d0418a44e89b232f5c9ba2f445dd5effb0fe9332fcb8d6c6b7113807b15a5b3ff1c6a008cffb5a505fdb83cf6a3e51d275443d8529f919d3e90cffd8711c18ef85333854dbce15156aa3f07865d9a3af3f008a6155dcc2de581c7ff3476a47a88197a7d9349af17c50ddb9a9418b39a0214e513dda99398e11f38820604a0423d0a18a8bd43e5d0c338882e637cdb5b45521cf4dc6bda14d9688b56db2db71515be2c8e8f140677a1f04b9180430a9db124cbd22a8e9cecba044c3ac0a20ef019f9bdaf69a7175eeef2af57819c1f9b070ac411d9cbb0dead392305a3a7a6112d825986d359586504c0ea9ca97c2feabb5f62e102e39bf9d9ed22ed6eed8f9e47f5482384cf31bbc4aa39fedda9042dfb926fdd20f2fd9ed8a50dedbd91c67cb9dcddc0";
		
		System.out.println("Verifier.verify(work, solution): " +  Verifier.verify(work, solution));
		boolean fitsTarget = EquihashUtil.fitsTarget(work, solution, "0004189374bc6a7ef9db22d0e5604189374bc6a7ef9db22d0e5604189374bc6a");
		System.out.println("EquihashUtil.fitsTarget(work, solution, target): " + fitsTarget);
	}

}
