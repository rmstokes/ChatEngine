package websocket.chat;

public class Client {
	
	private final String permID;
	private String username;
	private int sessionID;
	private int groupID;
	
	public Client (String senderID) {
		permID = senderID;
	}
	
	public String getPermID() {
		return permID;
	}

}
