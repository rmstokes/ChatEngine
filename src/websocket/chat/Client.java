package websocket.chat;

import javax.websocket.Session;

import util.CHAT_COLOR;
import util.ErrorCode;

public class Client {
	
	public final String permID;
	public String sessionID;
	public Session session;
	public ErrorCode errorCode = ErrorCode.None;
	
	public boolean isAdmin = false; //designates admin
	public boolean answerStatus = false; //used for answer (student)

	public String username;
	public int groupID = -1; //default means never joined a group
	public CHAT_COLOR chatColor;
	
	public Client (String senderID, String serverID) {
		permID = senderID+serverID;
		sessionID = senderID; //Not necessary
	}
	
	public String getPermID() {
		return permID;
	}
	
	public String IDString () {
		if (username!=null)
			return username + " ["+permID+"]"+ "{"+session.getId()+"}";
		else 
			return this.toString()+ " ["+permID+"]";
	}

}
