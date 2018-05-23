package victor.miner.stratum.out;

public class AuthoriseMessage extends OutMessage {

	private static final String MSG_TEMPLATE = "{\"id\":%d,\"method\":\"mining.authorize\",\"params\":[\"%s\",\"%s\"]}";	
	
	private String username, password;
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public AuthoriseMessage(int id, String username, String password) {
		this.setId(id);
		this.username = username;
		this.password = password;
	}
	
	public AuthoriseMessage(String username, String password) {
		this(2, username, password);
	}
	
	@Override
	public String toString() {
		return String.format(MSG_TEMPLATE, getId(), username, password);
	}
}
