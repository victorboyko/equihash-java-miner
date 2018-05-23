package victor.miner.stratum.in;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SubscribeRsMessage extends InMessage {
	
	private String nonceLeftPart;
	
	public String getNonceLeftPart() {
		return nonceLeftPart;
	}
	
	@Override
	protected void parseRemainder(JSONObject json) {
		nonceLeftPart = ((JSONArray)json.get("result")).get(1).toString();
	}
	
}
