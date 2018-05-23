package victor.miner.stratum.in;

import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import victor.miner.stratum.Message;

public class InMessage extends Message {
	
	private int errorCode;
	public int getErrorCode() {
		return errorCode;
	}
	
	private String errorMessage;
	public String getErrorMessage() {
		return errorMessage;
	}
	
	
	public InMessage parse(JSONObject json) {
		Object idObj = json.get("id");
		try {
			if (idObj != null) {
				String idStr = idObj.toString();
				setId(Integer.valueOf(idStr));
			}
		} catch (NumberFormatException e) {
			// no id is ok
		}
		Object errorJson = json.get("error");
		if (errorJson != null) {
			JSONObject errTuple = (JSONObject)errorJson;
			try {
				this.errorCode  = Integer.valueOf(errTuple.get(0).toString());
			} catch (NumberFormatException e) {
				// TODO: hmm, is it ok not having error code?
			}
			this.errorMessage = errTuple.size() > 1 ? errTuple.get(1).toString() : errTuple.get(0).toString();			
		} else {
			parseRemainder(json);
		}
		return this;
	}
	
	protected void parseRemainder(JSONObject json) {
		// it's ok not to have a remainder
	}

	public static InMessage parse(String mesStr, Map<Integer, InMessage> responses) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(mesStr);
		
		Object idObj = json.get("id");
		
		if (responses != null && responses.containsKey(json.get("id"))) {
			return responses.get(Integer.valueOf(json.get("id").toString())).parse(json);
		}
		
		if (idObj != null && Integer.valueOf(json.get("id").toString()) == 1) {
			return new SubscribeRsMessage().parse(json);
		}
		
		Object method = json.get("method");
		if ("mining.set_target".equals(method)) {
			return new SetTargetMessage().parse(json);
		}
		if ("mining.set_extranonce".equals(method)) {
			return new SetExtranonceMessage().parse(json);
		}
		if ("mining.notify".equals(method)) {
			return new NotifyMessage().parse(json);
		}
		if (method != null) {
			throw new IllegalArgumentException("unknown stratum method: " + method);
		}
		return new InMessage(); // TODO
	}
	
}
