package victor.miner.stratum.out;

public class ExtranonceSubscribe extends OutMessage {

	private static final String MSG_TEMPLATE = "{\"id\":%d,\"method\":\"mining.extranonce.subscribe\",\"params\":[]}";	
	
	public ExtranonceSubscribe(int id) {
		this.setId(id);
	}
	
	public ExtranonceSubscribe() {
		this(1);
	}
	
	@Override
	public String toString() {
		return String.format(MSG_TEMPLATE, getId());
	}
}
