package victor.miner.stratum.in;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class NotifyMessage extends InMessage {

	private String job_id, nversion, hash_prev_block, hash_merkle_root,
    	hash_reserved, ntime, nbits;
    	
    private boolean clean_jobs;
    
	@Override
	protected void parseRemainder(JSONObject json) {
		JSONArray params = (JSONArray)json.get("params");
		job_id = params.get(0).toString();
		nversion = params.get(1).toString();
		hash_prev_block = (params.get(2).toString());
		hash_merkle_root = params.get(3).toString();
		hash_reserved = params.get(4).toString();
		ntime = params.get(5).toString();
		nbits = params.get(6).toString();
		clean_jobs = Boolean.valueOf(params.get(7).toString());
	}

	public String getJob_id() {
		return job_id;
	}
	
	public String getNversion() {
		return nversion;
	}
	
	public String getHash_prev_block() {
		return hash_prev_block;
	}
	
	public String getHash_merkle_root() {
		return hash_merkle_root;
	}
	
	public String getHash_reserved() {
		return hash_reserved;
	}
	
	public String getNtime() {
		return ntime;
	}
	
	public String getNbits() {
		return nbits;
	}
	
	public boolean isClean_jobs() {
		return clean_jobs;
	}
	
}
