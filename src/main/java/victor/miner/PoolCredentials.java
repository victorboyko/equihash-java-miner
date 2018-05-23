package victor.miner;

public class PoolCredentials {
	
	public static final String helpMessage =
			  "--server            Stratum server only hostname or ip address.\n"
			+ "--port              Stratum server port.\n"
			+ "--user              Stratum user.\n"
			+ "--pass              Stratum password.\n";
	
	public String host, username, password;
	public int port;

	public static PoolCredentials getFromArgs(String[] args) {
		PoolCredentials creds = new PoolCredentials();
		
		for(int i = 0; i < args.length - 1; i+=2) {
			
			String paramName = args[i];
			if ("--server".equals(paramName)) {
				creds.host = args[i+1];
				continue;
			}
			if ("--port".equals(paramName)) {
				try {
					creds.port = Integer.valueOf(args[i+1]);
				} catch (NumberFormatException e) {
					// do thing, will check the completeness of parameters later on
				}
				continue;
			}
			if ("--user".equals(paramName)) {
				creds.username = args[i+1];
				continue;
			}
			if ("--pass".equals(paramName)) {
				creds.password = args[i+1];
				continue;
			}
		}
	
		if (creds.host == null || creds.port == 0 || creds.password == null || creds.username == null) {
			return null;
		}
		
		return creds;
	}
	
}
