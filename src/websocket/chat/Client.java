package websocket.chat;

import javax.websocket.Session;

import util.CHAT_COLOR;

public class Client {
	
	public final String permID;
	public String sessionID;
	public boolean isAdmin = false;

	public String username;
	public int groupID = -1; //default means never joined a group
	public CHAT_COLOR chatColor;
	public Session session;
	
	public Client (String senderID) {
		permID = senderID;
		sessionID = senderID; //Not necessary
	}
	
	public String getPermID() {
		return permID;
	}
	
	public String IDString () {
		if (username!=null)
			return username + " ["+permID+"]";
		else 
			return this.toString()+ " ["+permID+"]";
	}

}
