package victor.miner.stratum.in;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SetTargetMessage extends InMessage {

	private String target;
	
	public String getTarget() {
		return target;
	}
	
	@Override
	protected void parseRemainder(JSONObject json) {
		target = ((JSONArray)json.get("params")).get(0).toString();
	}
	
}
