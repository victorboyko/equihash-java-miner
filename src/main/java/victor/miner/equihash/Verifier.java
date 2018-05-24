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
		String nonceRight = "b94d9e73fd6091f7b5514bc77b860eb61af29969e1f49b3f934615";
		String nonce = nonceLeft + nonceRight;
		
		String work = version + prevhash + merkleRoot + reserved + ntime + nbits + nonce;
		//String solution = "fd400501273d950e9a4a6b7aea40383bcdf7fdbd525a3110160d52e9a518f34177a25191ef9b301ab3ea7a8f0910372a51ee516a79fe5466fb83f6bc77157fbf7e42278a0bc5f3619a276ec675fc3bfb754a36611eee0a0c7a43f3a8840fbfeb1e356fd5b20c29a32871d6c3357a2a45f4d9f913292f754407c32ee1b3255048af15422fab41c962ef93a5616f5d35b1869dfdde20e41599d9d13a97b0a4ea03624a13b7042cd455eb0ce3013692ecbe4a22a2ca6b094494e56c0f8db11f82606c782ea72beee04ddb8c18e0c3c77b96ca6139ec1511911213201b2ad532f1e8c9867f7a4aff603b26ee1bf84c8f72cd3d84c56122129365fe9d58471d82ba1505824b6cd556f9fb96348f00fdfea1616f4eb9e35123e5209130ee33af0758240a7fc743ae423e8e761cef5dd8ae14d46d874193d2de280813b984dfad8a2724db8e589353d8fc66671e5873494e5676fdfcd504426beb89879818a97a03f9bf3ee68dcbb09edc30199077832487eee379b71422aedb93adff3efd1362050a2663c26fa4f9fb63a61e3ad2546dcff0f1abb62c12c174cee97d535e0906240358b1adacc40eb61a09b37447f5448ace359221f16bfbbac10be737760537af353915f540e1cfe695fda27b5397a660de7c460a84d662296361e1798120b86be9f1daf6c679828133db6f142090c3f4891917ad3156f61a304b7ca340073082201c127b42dfefb278f2b30319e4ddf40de00804e9d05bcf3a1ce7c9e36078c4ffe9274b5637360fff54826f4e02e2eac7e332c2337a7ecf26b7ea001e7e9f5ea9151f42bc7b333032a5ed2f334d7c7f45098b1ec8d8524890a085a3888a41b780fa8a5894930e0d3cb45e22c2935784a678d6405059c20571e09b0a06fd16b058de3b1f830115751bf3a1110c51d95024239c30a057d593ce8f25c1ef477d1ae8493b5200034fa053c416d1914381862213664cc62e4eda1b6f106a2328aa624b69a230c41ee3b86a21bae2722c4e19cd7414bc187cdd64e92301dbdaa850e89ba7ddc050758cc88f57ba930ccee69ccad3bf6f32d8bfb62b04c127a05c18fe9139bb60706105169e169215f6d91a512cb6bc8b200cea6509a68d59ae62e3f1bed7e7058401894bc7a50cff44e34bdcb4b895446a33a7c43f80df1a56e1977f5fae47b478f6feca2d0f99ba43068ecb804212a993a397c0e25f524a543b37736d5f101421445ff270edd7efab4e6fe8a2876bb0bcfeb40c6682ab6a99d9b969cc16f565bd7d3b188d9bddbb36804e4c32908d6fdfc58ce4c17cab6b84c6fdb774087eeb062892013d0eae45098d66a44f7ec05e71ca0ec1d4aef5d6ac97d0af85b791ffd9be765177ebac10101e01de60d50142e8d2f050ed15b5821f158bc237c74a1ea368d4bd5e2c670b93cd5bb24ffd59ad3204ce3351cb21e9f75a76811d31c1daf1140a9a85890b2cdff9c060678beca4a232e02c6b40bba03456852207a1b75289db38d99f9361c64bfbad178217066228df9c881f5ddb18fa16c46fb14f84720e6e10c5230b53ee95f74f3b3c8183f1dc77bbfa3dd91679f40e33d64e10565a3a33b64715eefa64a481989954f1e02d422375ea50ad695d89454ad44e58497cf33152dc2dbc64658258efd2dd5d8642a6e1ba59ef739a2ff70802a48f9b47cd12a657f0bf48bfdd358cd6af5b0414940c3d90460bcd562998a54a76be6f65d6dbef8d1dd9a53e5933bb0dba9293bd80dc267a072a7235822f7fd4e28a1d66dfcee7a65e5efc825e54cadd8e0b18472369695381d2e2c4f9a288d78b2a918abd0d223b224e744e2c8bd5efd7686e22f2c3639dd13e907519349c32a60bacc7837d31eaca3a0df604019c961736bbdd5ddd6bc727edf8d48ec2e89019e8681f8940";
		String solution = "fd40050021e3b810c1924135def8704bd1f91f46c41f6d800a6738d55287fbd731b6447e694ccc6d8c1639be4f1a34dc283660bbffcebb553eaa408a67256a19d1452b85160fc80d9e893ebbb3d30a3df16e19b55391570589a115368ac950dcf39396c7d97851cffef93df41a35fd9c1dd5b99391bc4495e8f659317d0cdba3ca158b7b08438f60f9f594030e93d294b559f80bfa1a24100e7bd696d93f5075d2cb45220e991413b6516202b695442c56d175f7e68226f0d8dec108fb3068b91f079fe7e7e07ff7b14fc2c17e28934a8beb9f5e9e04caebeaa4acb3b1dc13e0688343270770183e11c31205a313162217e7f376e4e42d5fcf42af4a176adc0bd07c6189d056b9d8c726593473b4ce940e98e5bb12007e381f179887525733912ba691f97ab696ec330d580e6ad2b882e1e200f3132e748c74e2d03f453c19200ba8af0bf4cd4f7b527e5ddeed58d7ede812b70025c2096508f6b519716655723bda764aec1b18cc1723e8d64b57ab5b0f5763476edb981d79129292c00c208102fd978ab71624030ed76b757ac2837e4044129c3717110f2f41c39d12817bba1f81e5b1b4007c156ac1d1688625ee6959358e2ecf8f927ea3bafb361c7f79e64755ef79a364d5086939a816dd77b9fee824ac5306f6ca3d45056f272b577b983757d7de5cee4254b579d0d2c01114b3f445bda6ab2250823dc95c04e3a56776d1c7517e38c0689edf21387e7ecb3bf01e42d20346498ed49db462185ee9bc91f3cecfe544099ca31aef1ff07d191f49048bdc27768a935a010e103c7afa36f7f1d3d3a997661df83f6e0d101f60b00c43aea7f3b2cba5c87295e02aec2916c20d784de91e01b7ec721c7e7f68d443ab20b799f5f3cc906c293535c703db6026ab6cbfe8395a69148f598bbcc7464439eebca527f415f27f053359384ad6bf479d79ee013d673c95814ccfb3e35248215c7b969a78f832e00fbdf15587b0c65ba7e6388648f93753f5b4bff31513dbc9bea387401752a2a39d6bfb8e391b978c11fd183d8417918e7cf981af345f2d3c3806f1329f7409051c12c746eab3657ab0458ed0b498379184bf97931bc193b7448a57050d9114e25a50e2a97298b816800bebe571cc1c3fbb997dc1f22fba7d38d540155b5e33d8234bdcd6ba5d01aa94a7f6dcb8a2352dde375708eca9dd03b5d933ee662294b8a5c6414236cb5f2374e51c0133a1f1d77e1398cb376f19de94087cce0f0b02ab7be6ac79eb701fc3c5289e2dcd8d335aaaf321a50a0db988e830e03cb2e830b2b95ce1fe9ec15d0915b30f92c7a111f5f9828bd5f98715690dcd1d9d0b93e9e3204b5436c6e1f1cbadea708d11b52fb96313fe5ee5987b8d5bdd79d7db005280c703b0390af117e34ac8191cc1a329c6b3f0127ab216416475631f084d39b566e1c10959ead61cb749cd11d871fc665d086f5776a762dbe9e16ca753ee596291d7fa14add81a1ee7cb06926ef71f3131c8c3377b9b0f25dac72628f9a723299e25e9d8386642724378668a321beed50e0f2dde7b51b14b50a721d7d12964bd0e9e7fd843228a2b8edf5656c1adf1c3dbcdbdb656c7e93cd250174969dcb49c8444e89e467f536a831e57afdf23692342e692eb8bfa7f729538d12b4ec363e19d1f94d410b5cf70b39e900f0996b30f3d7a08e2cef939bdc417fb1cf4748ef58efa96862ee9bfeee59ff53f01d51559374e2c9547d3a49af6078af623f75b18de7f3152474c7bc25b3d651887186945443e4678c03659ec10d3d199dc7bec85f12d25136d5d994764d57f4606774cb4c75830bfd1e7e808c2286a0e9f36cadacff915e3bc42eb5e7173d45c12ffc3cdac94c62af9119227b8e3f7ed18067f3cdbb4464a7d6d0dd3142efb26";
		
		System.out.println("Verifier.verify(work, solution): " +  Verifier.verify(work, solution));
		boolean fitsTarget = EquihashUtil.fitsTarget(work, solution, "0004189374bc6a7ef9db22d0e5604189374bc6a7ef9db22d0e5604189374bc6a");
		System.out.println("EquihashUtil.fitsTarget(work, solution, target): " + fitsTarget);
	}

}
