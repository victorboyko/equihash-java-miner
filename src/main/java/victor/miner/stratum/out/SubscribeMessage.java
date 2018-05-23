package victor.miner.stratum.out;

public class SubscribeMessage extends OutMessage {

	private static final String MSG_TEMPLATE = "{\"id\":%d,\"method\":\"mining.subscribe\",\"params\":[\"Victor's miner\",null,\"%s\",\"%d\"]}";	
	private String host;
	private int port;
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	
	public SubscribeMessage(int id, String host, int port) {
		this.setId(id);
		this.host = host;
		this.port = port;
	}
	
	public SubscribeMessage(String host, int port) {
		this(1, host, port);
	}
	
	@Override
	public String toString() {
		return String.format(MSG_TEMPLATE, getId(), host, port);
	}
	
}
