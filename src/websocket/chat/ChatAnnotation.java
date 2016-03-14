/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package websocket.chat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import util.HTMLFilter;
import util.MessageType;

@ServerEndpoint(value = "/{path}")
public class ChatAnnotation {

	private static final Log log = LogFactory.getLog(ChatAnnotation.class);

	//private static final String GUEST_PREFIX = "Guest";
	//private static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX");
	//private static final AtomicInteger connectionIds = new AtomicInteger(0);
	
	//private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<>();
	private static final ArrayList<Session> connectedSessions = new ArrayList<Session>();
	private static final ArrayList<Client> userList = new ArrayList<Client>();
	private static FileLogger fileLogger;// = new FileLogger();
	
	private static GroupManager groupManager;

	// private static String adminID = null;
	private static boolean adminCreatedGroups = false;
	//private static Hashtable<Integer, HashSet<String>> groupTable = new Hashtable<Integer, HashSet<String>>();

	// create color vector to assign colors to members of each chat group
	// format: HashTable<Group#, Hashtable<color, count>> ex. Group 1: red->1;
	// Group1: blue->0
	//private static Hashtable<Integer, Hashtable<String, Integer>> colorVector = new Hashtable<Integer, Hashtable<String, Integer>>();

	// create clientColor variable to store username color for each client
	//private static Hashtable<String, String> clientColor = new Hashtable<String, String>();
	
	
	//private String nickname;
	private Session session;
	private Client userClient;

	public ChatAnnotation() {
		// a new ChatAnnotation object is created for every Client page that
		// connects to server.
		System.out.println("Chat Annotation Constructor");
	}

	@OnOpen
	public void start(Session session, @PathParam("path") String path) throws Exception {
		// onopen called when client side websocket initiates connection to this
		// server.
		// path argument identifies which client page is on other end
		// for 'landing' page connections add the ChatAnnotation object to
		// static list.
		System.out.println("inside start - opened Websocket Connection at /"+path);
		//System.out.println("path: " + path);
		
		this.session = session;
		System.out.println("current sessionID: " + session.getId());
		
		switch (path) {
		case "admin":
			System.out.println("admin connected.");
			//Send information about groups (num, exists) so the admin/login page can inform
			//the user and act properly
			sendXMLCheckGroups(session.getId());
			synchronized (connectedSessions) {
				connectedSessions.add(this.session);
			}
			//connections.add(this);
			return;
		case "login":
			// if admin has not created groups then don't allow client to
			// connect.
			System.out.println("login page connected.");
			synchronized (connectedSessions) {
				connectedSessions.add(this.session);
			}
			//connections.add(this);
			sendXMLCheckGroups(session.getId());
			return;
		case "chat":
			// if admin has not created groups then don't allow client to
			// connect.
			System.out.println("chat page connected.");
			System.out.println("adminCreatedGroups: " + adminCreatedGroups);
			synchronized (connectedSessions) {
				connectedSessions.add(this.session);
			}
			//connections.add(this);
			return;
		case "landing":
			System.out.println("landing page connected.");
			// save connections originating from landing page.
			//connections.add(this);
			return;
		default:
			return;

		}

	}

	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {
		System.out.println("inside end method... Closing webSocket @ "+path);
		//connections.remove(this);
		synchronized (connectedSessions) {
			connectedSessions.remove(this.session);
		}
		
		if (!session.isOpen()) return;
		if (path.equals("chat")) {
			if(userClient==null) return;
			
			System.out.println("Disconnecting permID: "+userClient.permID +" -> group "+userClient.groupID);

			if(userClient.groupID==0) return; //invalid user using an old ID
			int bGroupID = userClient.groupID; //save groupID before removing from group

			//try {
			//remove user from group
			//HashSet<Client> group = groupManager.getGroup(userClient.groupID);
			Set<Client> group = groupManager.getGroup(userClient.groupID);
			userClient.groupID = 0; //technically null, no zero group
			group.remove(userClient);
			
			/*} catch (Exception e) {
				System.out.println("Error occurred when closing webSocket/chat");
				System.out.println("userClient -> "+userClient.toString()+" group ->");
				log.error(e);
			}*/
			
			//Tell users about successful exit
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			//Element exitMsg = getQuickElement("message");
			Element exitMsg = doc.createElement("message");
			exitMsg.setAttribute("type", "alert");
			exitMsg.setAttribute("senderID", this.userClient.permID);
			exitMsg.setAttribute("groupNumber", Integer.toString(bGroupID));
			exitMsg.setAttribute("senderColor", userClient.chatColor.toString());
			exitMsg.setAttribute("sendername", userClient.username);
			exitMsg.appendChild(doc.createElement("text"));
			exitMsg.getFirstChild().setTextContent(userClient.username+" has left the group.");
			//exitMsg.getFirstChild().setTextContent("<span style=\"color:"+ userClient.chatColor.toString()+"\">"+
					//userClient.username +" </span> has left the group.");
			
			if(group.size()==0) {
				//need to log the room exit, but not broadcast cause group is empty
				long dateTimestamp = new Date().getTime();
				exitMsg.setAttribute("timestamp", Long.toString(dateTimestamp));
				synchronized (fileLogger.getLoggedClientMessages()) {
					fileLogger.captureMessage(exitMsg);
				}
				return;
			}
			
			broadcastGroup(exitMsg, bGroupID);
			sendGroupMembers(bGroupID); //update group members
			return;
		}
		
		
		/*if (!path.equals("chat")) return;
		
		//decrement colorVector value 
		//format: HashTable<Group#, Hashtable<color, count>> ex. Group 1: red->1;
		String senderID = session.getId();
		int originalGroupNumber = getSenderGroup(senderID);
		String username = this.nickname;
		System.out.println("username: "+username);
		
		String color = clientColor.get(username);
		System.out.println("color: "+color);
		
		Hashtable<String, Integer> colorCount = colorVector.get(originalGroupNumber);
		int count = colorCount.get(color);
		count--;
		colorCount.put(color, count);
		colorVector.put(originalGroupNumber, colorCount);
		
		//remove username from clientColor
		//format: Hashtable<nickname, color> pair in clientColor variable
		clientColor.remove(username);
		
		
		// send message to chat group users		
		String message = "<message type='alert' senderID='" + session.getId() + "' groupNumber='" + originalGroupNumber
				+ "'><text>" + username + " has left group.</text></message>";
		
		  try { 
			  broadcast(message); 
		  } catch (Exception e) { 
			  e.printStackTrace(); 
		  }*/
		 
	}

	@OnMessage
	public void incoming(String message) throws Exception {
		// parse xml message and send broadcast to group.
		// may handle certain types of messages differently though
		
		// parse xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		StringReader sr = new StringReader(message);
		InputSource is = new InputSource(sr);
		Document doc = builder.parse(is); //& crashes here

		// get message type
		Element element = doc.getDocumentElement();
		String messageType = element.getAttribute("type");

		String senderID = null; 
		
		if (messageType.equals(MessageType.Landing_SetUserType)) {
			//From Landing page- Set user type based on client input
			senderID = session.getId();
			System.out.println("message: " + message);
			System.out.println("element: " + element.getTextContent());
			//System.out.println("senderID: " + senderID);
			
			//Set the permanent ID of the client here; add to number of recent users
			this.userClient = new Client(senderID);
			System.out.println("permID: "+userClient.permID+" -> "+ userClient.toString());
			userList.add(this.userClient);
			
			String userType = element.getTextContent();
			switch (userType) {
			case "Login":
				//go to the student login page
				sendXMLRedirect("login", userClient.getPermID());
				return;
				
			case "Admin":
				//go to admin page
				sendXMLRedirect("admin", userClient.getPermID());
				return;
			case "PermIDGet":
				//Work around for login page
				System.out.println("Login page requesting permanent ID");
				sendXMLMessage("permID", userClient.getPermID());
				return;
			default:
				System.out.println("Invalid userType input");
				return;
			}
		}
		
		//If there is no Permanent ID, redirect client to Landing now, 
		//no future webpages should work without a permID assigned from landing page
		//Get the senderID and Client and link to this ChatAnnotation class
		if (userClient==null) { //Only retrieved once when the user accesses advanced functions
			senderID = element.getAttribute("senderID");
			
			//Use senderID to find Client and connect to class
			for (int i=0; i<userList.size(); i++) {
				Client user = userList.get(i);
				if (user.permID.equals(senderID)) {
					this.userClient = user;
					break;
				}
			}
			
			//If it can't find a client for this user, kick back to landing to make new one
			//If the server is restarted but browser window isnt closed, this happens a lot
			if (senderID=="" || senderID==null || userClient == null) {
				sendXMLMessage(MessageType.Util_NoPermID, "NaN");
				return;
			}
			//Stop outdated clients from stealing IDs
			if(userClient.session!=null && userClient.session.isOpen()) {
				//outdated ID, force user to remake ID
				sendXMLMessage(MessageType.Util_NoPermID, "NaN");
				return;
			}
			
			//Get current session ID
			userClient.sessionID = this.session.getId();
			//Save the session too- easier to retrieve for group broadcast
			userClient.session = this.session;
		}
		
		//I'll use userClient from now on to refer to the client computer instead of ID
		if (messageType.equals(MessageType.Admin_GroupCreation)) {
			//From ADMIN page, called when accessing group creation
			
			int numGroups = Integer.parseInt(element.getTextContent());
			String instructor = element.getAttribute("instructor");
			
			if(numGroups == 0) return; //Ignore invalid messages
			
			groupManager = new GroupManager(numGroups, instructor);
			//End Filelogger and recreate new one
			if (fileLogger!=null)
				fileLogger.destroy();
			fileLogger = new FileLogger();
			//Remember to stop timer (causes memory leak?)
			adminCreatedGroups = true;
			
			broadcastCheckGroups(); //Broadcast change of groups to all users
			sendXMLMessage("groupsCreated", userClient.permID); //just additional confirmation for admin page
			return;
			
		} else if (messageType.equals(MessageType.Login_JoinGroup)) {
			//FROM LOGIN PAGE, add user to group
			System.out.println("joinGroup message received");
			System.out.println("permID: "+userClient.permID+" -> "+ userClient.toString());
			
			int groupNumber = Integer.parseInt(element.getTextContent());
			System.out.println("Joining group: "+groupNumber);

			//If no groups, send alert to tell user
			if (!adminCreatedGroups) {
				sendXMLMessage("alert", userClient.permID,
						"No groups have currently been created. Please wait until an Admin creates groups to join.");
				System.out.println("joinGroup failed: No groups exist.");
				return;
			}
					
			
			//Concurrency- though its unlikely multiple users will connect at once
			boolean joinGroupErr;
			//synchronized (this) {
				joinGroupErr = groupManager.joinGroup(groupNumber, userClient); //cant add duplicates
			//}
			if (!joinGroupErr) {
				sendXMLMessage("alert", userClient.permID, 
						"Could not join Group "+groupNumber+". Please ask for assistance.");
			}
			
			//If successful, initialize groupMember info, then send them to chat page
			System.out.println("UserClient ID "+userClient.permID+" connected to Group "+groupNumber);
			
			userClient.username = element.getAttribute("username");
			userClient.groupID = groupNumber;
			groupManager.assignChatColor(userClient);
			
			System.out.println("Username: "+userClient.username+"-> Group: "+groupNumber+"-> color: "+userClient.chatColor.toString());
			sendXMLMessage("displayChat", userClient.permID);
			return;
			
		} else if (messageType.equals(MessageType.Chat_JoinChat)) {
			//Redirect to login if no groups exist
			if (!adminCreatedGroups) {
				sendXMLMessage("alert", userClient.permID, 
						"Could not join Chat, no groups exist. \n Redirecting you to the login page.");
				sendXMLRedirect("login", userClient.permID);
				return;
			}
			
			//If user refreshed page, kick back to login
			if (userClient.groupID==0) {
				sendXMLRedirect("login", userClient.permID);
				return;
			}
			//New user joining the chat room of group X
			System.out.println("New user joining the chatroom!");
			//All information is already in the userClient
			System.out.println("currID: "+userClient.sessionID+" -> permID: "+userClient.permID);
			System.out.println(userClient.toString()+" -> Group "+userClient.groupID);
			
			//Create a new chat message for entering session
			/*String broadMsg = "<message type='alert' senderID='" +senderID +
					"' groupNumber='"+userClient.groupID + "' senderColor='" + userClient.chatColor.toString() +
					"'><text>" +userClient.username + "has joined the group.</text></message>";*/
			
			//Instead of making a literal text string, we will create an Element because
			//its a little easier to add things to it programmatically
			Element broadMsg = doc.createElement("message");
			broadMsg.setAttribute("type", "alert");
			broadMsg.setAttribute("senderID", senderID);
			broadMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			//sending senderColor/name for client side beautification
			broadMsg.setAttribute("senderColor", userClient.chatColor.toString());
			broadMsg.setAttribute("sendername", userClient.username); //Not needed for alert messages
			broadMsg.appendChild(doc.createElement("text"));
			broadMsg.getFirstChild().setTextContent(userClient.username +" has joined the group.");
			//broadMsg.getFirstChild().setTextContent("<span style=\"color:"+ userClient.chatColor.toString()+"\">"+
					//userClient.username +" </span> has joined the group.");
			
			System.out.println("Broadcasting user join message via alert");
			//System.out.println("Broadcast Message: "+convertXMLtoString(broadMsg));

			//getChatHistory and send to client
			getChatHistory(userClient);
			
			//Then broadcast joinMessage to user
			broadcastGroup(broadMsg, userClient.groupID);
			
			//Send list of group members
			sendGroupMembers(userClient.groupID);
			
			//return;
		} else if (messageType.equals(MessageType.Chat_Typing) || messageType.equals(MessageType.Chat_Chat)) {
			//From Chat- messages in the chat room
			
			if(userClient.groupID==0) {
				//user has technically disconnected from chat?
			}
			//Add groupMember information to element - senderID is already there
			//Could add this stuff in broadcast method... but interferes with alert chat msgType...
			//Timestamp will be added in broadcastGroup method
			element.setAttribute("sendername", userClient.username);
			element.setAttribute("senderColor", userClient.chatColor.toString());
			element.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			element.setAttribute("sessionID", userClient.sessionID);
			
			System.out.println("Broadcasting Chat/Typing Message: "+convertXMLtoString(element));
			broadcastGroup(element, userClient.groupID);
			return;
		}
		//END OF REFACTORING
		
		/*if (messageType.equals("typing") || messageType.equals("chat")) {//FROM CHAT
			
			//Capture the logging for chat here
			
			senderID = element.getAttribute("senderID");
			
			// transform the updated document into a string before broadcasting
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			message = writer.toString();
			System.out.println("broadcasting message");
			
			// assign senderColor attribute before broadcasting
			String senderColor = getSenderColor(senderID);
			element.setAttribute("senderColor", senderColor);
			broadcast(message);
		} else if (messageType.equals("groupCreation")) {//FROM ADMIN
			// this is message from admin page to create groups
			int numGroups = Integer.parseInt(element.getTextContent());
			System.out.println("numGroups: " + numGroups);
			
			// create the groups
			// can check here if admin is still connected to prevent multiple admins
			
			//can send a confirmation message if groups already exist
			//Error message if no. of groups is 0

			for (int key = 1; key <= numGroups; key++) {
				// create empty linkedlists for every key.
				HashSet<String> value = new HashSet<String>();
				groupTable.put(key, value);
			}
			adminCreatedGroups = true;
			System.out.println("groups created");

			// initialize colorCount variable with the color names and the count
			// key value: <color, count>
			int count = 0;
			Hashtable<String, Integer> colorCount = new Hashtable<String, Integer>();
			colorCount.put("red", count);
			colorCount.put("green", count);
			colorCount.put("blue", count);
			colorCount.put("orange", count);

			// initialize group numbers for colorVector
			for (int key = 1; key <= numGroups; key++) {
				// make each group# have it's own colors to choose from
				colorVector.put(key, colorCount);
			}

			// send message to client to invoke a new web page that displays
			// group creation confirmation and link to login page possibly
			senderID = element.getAttribute("senderID");
			String msg = "<message type='groupsCreated' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg);

//		} else if (messageType.equals("setUserType")) {//FROM LANDING
//			// this is a message from landing page drop down menu
//			// session contains the initial client id.
//
//			/***** this is where each client's id is assigned.
//
//			senderID = session.getId();
//			System.out.println("message: " + message);
//			System.out.println("element: " + element.getTextContent());
//			System.out.println("senderID: " + senderID);
//
//			//Here the current SessionID becomes the permanentID for this Client
//			this.userClient = new Client(senderID); 
//			
//			String userType = element.getTextContent();
//
//			switch (userType) {
//			case "Login":
//				// return student login page
//				String msg1 = "<message type='displayLogin' senderID='" + senderID + "'></message>";
//				session.getBasicRemote().sendText(msg1);
//				break;
//			case "Create Groups":
//				// return admin page
//				String msg2 = "<message type='displayAdmin' senderID='" + senderID + "'></message>";
//				session.getBasicRemote().sendText(msg2);
//				// System.out.println("sent html to client.");
//				break;
//			default:
//				System.out.println("Error: check userType");
//				return;
//			}
		} else if (messageType.equals("joinGroup")) { //FROM LOGIN PAGE
			// add user to proper group
			//String nickname = element.getAttribute("username");
			nickname = element.getAttribute("username");
			// nickname = username;
			senderID = element.getAttribute("senderID");
			System.out.println("joinGroup message received.");
			System.out.println("senderID: " + senderID);
			
			String groupNumber = element.getTextContent();
			System.out.println("joining group: " + groupNumber);
			
			HashSet<String> group = groupTable.get(Integer.parseInt(groupNumber));
			System.out.println("retrieved group list");
			boolean addedToList = group.add(senderID);

			// send message to move client browser to chat page
			String msg3 = "<message type='displayChat' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg3);
			System.out.println("addedToList: " + addedToList);

		} else if (messageType.equals("requestID")) {//FROM ALL PAGES EXCEPT LANDING
			System.out.println("Client requesting new ID- Illegal action");
			senderID = session.getId();
			
			//So we're not on the landing page, so if the user is not illegal
			String msg4 = "<message type='alert' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg4);
		} else if (messageType.equals("broadcast")) {//FROM CHAT
			// broadcast message means user has just joined chat group.
			// map the current session's sender id to the connection id
			// established on initial
			// connection from landing page

			// send message to group indicating who has just joined.
			System.out.println("broadcast message type detected.");
			//String nickname = element.getAttribute("username");
			nickname = element.getAttribute("username");
			senderID = element.getAttribute("senderID"); // original senderID
			System.out.println("senderID: " + senderID);

			// establish link between current session.id() and original senderID
			// these values will be different so they need to be mapped to one
			// another
			// so that we can respond to the proper users within a group

			// place new senderID into group and remove old senderID from group
			Integer originalGroupNumber = getSenderGroup(senderID);
			System.out.println("original group number: " + originalGroupNumber);
			HashSet<String> group = groupTable.get(originalGroupNumber);
			group.remove(senderID);
			group.add(session.getId());

			// for each <group, client> pair assign a color from colorVector
			// (based on the minimum count in the colorVector)			
			Hashtable<String, Integer> colorAndValueTable = colorVector.get(originalGroupNumber);
			// loop through colorAndValueTable to find color that has minimum
			// value.
			Set<String> keys = colorAndValueTable.keySet();
			System.out.println("colorVector keyset size: " + keys.size());
			int min = 999;// sentinel value
			String senderColor = "";
			for (String color : keys) {
				int colorCount = colorAndValueTable.get(color);
				if (colorCount < min) {
					min = colorCount;
					senderColor = color;
				}
			}
			// increment color variable in colorVector<Group#,
			// Hashtable<senderColor, count>>
			int count = colorVector.get(originalGroupNumber).get(senderColor);
			count++;
			colorVector.get(originalGroupNumber).put(senderColor, count);
			System.out.println("sender color: " + senderColor);
			
			//store the <nickname, color> pair in clientColor variable
			clientColor.put(nickname, senderColor);

			String updatedID = session.getId();

			// send message to group that new user has joined session.
			String msg5 = "<message type='alert' senderID='" + senderID + "' updatedID='" + updatedID
					+ "' groupNumber='" + originalGroupNumber + "' senderColor='" + senderColor + "'><text>" + nickname
					+ " has joined group.</text></message>";
			System.out.println("calling broadcast with alert message.");
			broadcast(msg5);
		}*/
		
		//Capture message here in FileLogger class
	}

	@OnError
	public void onError(Throwable t) throws Throwable {
		log.error("Chat Error: " + t.toString(), t);
	}

	private void broadcastGroup(Element msg, int groupID)  throws Exception {
		//Its better to import the message in an XML Element form due to the manipulation &
		//attributes we have to derive from it
		//Assume ChatAnnotation has added the necessary elements
		
		if (groupID==0) {
			session.close();
			return; //There's an issue where refreshing the page removes the user from the group
			//but only after a chat message, should be resolved now
		}
		
		//Capture message in FileLogger here
		long dateTimestamp = new Date().getTime();
		//String dateTimestamp = serverDateFormatter.format(new Date());
		//DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
		//dateTimestamp = df.format(new Date());
		
		//Time in milliseconds is the easiest to translate in JS without losing important information
		msg.setAttribute("timestamp", Long.toString(dateTimestamp));
		synchronized (fileLogger.getLoggedClientMessages()) {
			fileLogger.captureMessage(msg);
		}
		
		System.out.println("Broadcasting message to group "+groupID);
		System.out.println("Broadcast Message: "+convertXMLtoString(msg));
		
		
		//HashSet<Client> group = groupManager.getGroup(groupID);
		Set<Client> group = groupManager.getGroup(groupID);
		
		//Get each client and push message to them
		String xmlStr = convertXMLtoString(msg);
		//synchronized (group) {
		for (Client gClient : group) {
			try { 
				//synchronized (gClient) { //
					gClient.session.getBasicRemote().sendText(xmlStr);
					System.out.println("Broadcast to groupMember -> " +gClient.toString());
				//}
			//a ton of error handling for lost messages
			} catch (Exception e) {
				log.debug("Chat Error: Failed to send message to client", e);
				group.remove(gClient);
				try {
					gClient.session.close();
				} catch (Exception ex) {
					log.error("Server Error: Could not disconnect client");
				}
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.newDocument();
				Element disconnectMsg = doc.createElement("message");
				
				disconnectMsg.setAttribute("type", "alert");
				disconnectMsg.setAttribute("senderId", "Server Info");
				disconnectMsg.setAttribute("groupNumber", Integer.toString(groupID));
				//disconnectMsg.setAttribute("senderColor", GroupManager.SERVER_COLOR.toString());
				disconnectMsg.appendChild(doc.createElement("text"));
				disconnectMsg.getFirstChild().setTextContent(gClient.username + " has disconnected from the group.");
				broadcastGroup(disconnectMsg, groupID);

			}//end of try catch		
		}//end of for - for each group Member
		//}//end of synchronized group
		
		
	} //End of broadcastGroup
	
	/*private void broadcast(String msg) throws Exception {
		// broadcast message to members of same group.
		System.out.println("inside broadcast...");
		System.out.println("broadcast msg: " + msg);
		// access xml Document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		System.out.println("after factory...");
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		System.out.println("after builder...");
		StringReader sr = new StringReader(msg);
		System.out.println("after sr...");
		InputSource is = new InputSource(sr);
		System.out.println("after is...");
		Document doc = builder.parse(is);
		System.out.println("after doc...");

		System.out.println("**adminCreatedGroups: " + adminCreatedGroups);

		Element element = doc.getDocumentElement(); // message element
		String senderID = null;
		if (!adminCreatedGroups) {
			senderID = session.getId();
			element.setAttribute("senderID", senderID);
		} else {
			senderID = element.getAttribute("senderID");
		}

		System.out.println("senderID: " + senderID);
		Integer senderGroup = getSenderGroup(senderID);

		// if the message is an initial connection from chat client
		// it will have an attribute called updatedID
		// in those cases, update the connections list with new info
		if (element.getAttribute("updatedID") != "") {
			System.out.println("initial connection from chat page detected");
			// set senderGroup based on xml attribute
			senderGroup = Integer.parseInt(element.getAttribute("groupNumber"));
			// update connection list with new client identification
			connections.add(this);
			// loop through connections and remove old client connection
			for (ChatAnnotation client : connections) {
				String sessionID = client.session.getId();
				if (sessionID.equals(senderID))
					connections.remove(client);
			}

		}
		System.out.println("senderGroup: " + senderGroup);
		System.out.println("updatedID: " + element.getAttribute("updatedID"));
		// set variable to store clientIDs for senderGroup
		HashSet<String> broadcastGroupMembers = groupTable.get(senderGroup);

		// set senderColor attribute in message
		
		 * String sendername = element.getAttribute("sendername"); String color
		 * = clientColor.get(sendername); element.setAttribute("senderColor",
		 * color);
		 

		// loop through all clients in connections list and send message to them
		// if their id is in broadcastGroup
		for (ChatAnnotation client : connections) {
			for (String id : broadcastGroupMembers) {
				// compare id from broadcastGroup to clientID
				System.out.println("clientID: " + client.session.getId());
				System.out.println("broadcastGroupID: " + id);
				if (!id.equals(client.session.getId()))
					continue;

				// transform the updated document (the one with clientID and
				// senderID) into a string before broadcasting
				DOMSource domSource = new DOMSource(doc);
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.transform(domSource, result);
				msg = writer.toString();
				try {
					// System.out.println("****broadcasting to clientid:
					// ****"+client.session.getId());
					synchronized (client) {
						System.out.println("msg: " + msg);
						System.out.println("client.session.getId(): " + client.session.getId());
						client.session.getBasicRemote().sendText(msg);
					}
				} catch (IOException e) {
					log.debug("Chat Error: Failed to send message to client", e);
					connections.remove(client);
					try {
						client.session.close();
					} catch (IOException e1) {
						// Ignore
					}
					String message = String.format("* %s %s", client.nickname, "has been disconnected.");
					broadcast(message);
				}
			}
		}
	}*/

	/*private Integer getSenderGroup(String senderID) {
		// find group of message sender
		Integer senderGroup = null;
		Set<Integer> keys = groupTable.keySet();
		for (Integer groupNumber : keys) {
			// look for senderID in each group's HashSet
			if (senderGroup != null)
				break;
			HashSet<String> group = groupTable.get(groupNumber);
			for (String clientID : group) {
				System.out.println("groupNumber: " + groupNumber + " clientID: " + clientID);
				System.out.println("senderID: " + senderID);
				if (clientID.equals(senderID)) {
					// sender group number located
					System.out.println("match in group number: " + groupNumber);
					senderGroup = groupNumber;
					return senderGroup;
				}
			}
		}
		return senderGroup;
	}*/

	/*private String getSenderColor(String senderID) {
		Integer originalGroupNumber = getSenderGroup(senderID);
		Hashtable<String, Integer> colorAndValueTable = colorVector.get(originalGroupNumber);
		// loop through colorAndValueTable to find color that has minimum value.
		Set<String> keys = colorAndValueTable.keySet();		
		int min = 999;// sentinel value
		String senderColor = "";
		for (String color : keys) {
			int colorCount = colorAndValueTable.get(color);
			if (colorCount < min) {
				min = colorCount;
				senderColor = color;
			}
		}
		System.out.println("senderColor assigned: "+senderColor);
		return senderColor;
	}*/
	
	//Send simple message in xml form
	private void sendXMLMessage(String msgType, String senderID) throws IOException {
		String msg =  "<message type='"+msgType+"' senderID='" + senderID + "'></message>";
		session.getBasicRemote().sendText(msg);
	}
	
	private void sendXMLMessage(String msgType, String senderID, String info) throws IOException {
		String msg =  "<message type='"+msgType+"' senderID='" + senderID + "'>"+ info + "</message>";
		session.getBasicRemote().sendText(msg);
	}
	
	//Send a redirect message with path
	private void sendXMLRedirect(String path, String senderID) throws IOException {
		String msg =  "<message type='redirect' path='" + path + "' senderID='" + senderID +"'></message>";
		session.getBasicRemote().sendText(msg);
		//return msg;
	}
	
	//Message to send data about group. Can integrate with regular msg but requires client side changes too
	private void sendXMLCheckGroups(String senderID) throws IOException {
		String groups = adminCreatedGroups ? Integer.toString(groupManager.getNumOfGroups()) : "Not Created";
		System.out.println("Sent Group Info - num: "+groups+" to "+connectedSessions.size()+" clients");
		String msg = "<message type='checkGroups' checkGroups='" + groups + "' senderID='" + senderID +"'></message>";
		session.getBasicRemote().sendText(msg);
	}
	
	//Send to all clients currently connected to server
	private void broadcastCheckGroups() throws IOException {
		String groups = adminCreatedGroups ? Integer.toString(groupManager.getNumOfGroups()) : "Not Created";
		String msg = "<message type='checkGroups' checkGroups='" + groups + "' senderID='Server Broadcast'></message>";
		//send to everyone
		synchronized (connectedSessions) {
		for (Session s : connectedSessions) {
			if (!s.isOpen()) continue;
			s.getBasicRemote().sendText(msg);
		}
		}//end of synchronized

		System.out.println("Broadcast Group Info - num: "+groups+" to "+connectedSessions.size()+" clients");
	}
	
	//XML to String
	public static String convertXMLtoString(Element node) throws Exception {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		String str = serializer.writeToString(node);
		return str;
		
		/*DOMSource domSource = new DOMSource(node);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		String msg = writer.toString();
		return msg;*/
	}
	
	//Quickly get an element without all the initialization- hopefully I don't need doc reference
	//This seems to cause runtime errors when multiple elements are needed, so not using this
	private Element getQuickElement(String elementName) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		Element e = doc.createElement(elementName);
		return e;
	}
	
	//send chat history messages to client
	private void getChatHistory(Client userClient) throws Exception {
		//get the copy/list of chat elements from the group this client is joining
		//Read-only operation & iterator; Doesnt need to be synchronized
		ArrayList<Element> chatHis = fileLogger.getLoggedClientMessages();
		//loop through loggedMessages and send anything from the same group
		for (int i=0; i<chatHis.size(); i++) {
			//Element chatMsg = (Element) chatHis.get(i).cloneNode(true);
			Element chatMsg = (Element) chatHis.get(i);
			//If chat isnt from same group, skip message
			if (userClient.groupID != Integer.parseInt(chatMsg.getAttribute("groupNumber")) 
					|| !chatMsg.getAttribute("type").equals("chat"))  //only get chat messages
				continue;
			chatMsg.setAttribute("chatHistory", "chatHistory"); //add extra value for fancy purposes
			
			System.out.println("Chat History message copy:"+convertXMLtoString(chatMsg));
			session.getBasicRemote().sendText(convertXMLtoString(chatMsg));
		}
	}
	
	private void sendGroupMembers(int groupID) throws Exception {
		//This sends a list of group members NAMES and colors
		//For UI purposes only
		System.out.println("Sending list of group members for group "+groupID);
		//Cant use an attribute so nodes will do
		String msg = "<message type='lGroupMembers'>";
		//HashSet<Client> group = groupManager.getGroup(groupID);
		Set<Client> group = groupManager.getGroup(groupID);
		
		for (Client c : group) {
			msg = msg.concat("<member senderColor='"+c.chatColor.toString()+"'>"
					+c.username+"</member>");
		}
		msg = msg.concat("</message>");

		System.out.println("Sending "+msg);
		
		for (Client c : group) {
			c.session.getBasicRemote().sendText(msg);
		}
		
	}
}
