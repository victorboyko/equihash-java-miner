package victor.miner.stratum.in;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SetExtranonceMessage extends InMessage {

	private String extranonce;
	
	public String getExtranonce() {
		return extranonce;
	}
	
	@Override
	protected void parseRemainder(JSONObject json) {
		this.extranonce = ((JSONArray)json.get("params")).get(0).toString();
	}
	
	
}
