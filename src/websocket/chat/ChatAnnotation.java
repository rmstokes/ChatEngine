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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
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

@WebListener
@ServerEndpoint(value = "/{path}")
public class ChatAnnotation implements ServletContextListener{

	private static final Log log = LogFactory.getLog(ChatAnnotation.class);

	//private static final String GUEST_PREFIX = "Guest";
	private static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	//private static final AtomicInteger connectionIds = new AtomicInteger(0);
	
	//private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<>();
	private static final ArrayList<Session> connectedSessions = new ArrayList<Session>();
	private static final ArrayList<Client> userList = new ArrayList<Client>();
	
	private static FileLogger fileLogger;// = new FileLogger();
	private static GroupManager groupManager;
	private static Timer userCullTimer;

	private static boolean adminCreatedGroups = false;

	private static int groupIteration = 0;
	private static String logPath = "log\\";
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
		//System.out.println("Chat Annotation Constructor");
	}

	@OnOpen
	public void start(Session session, @PathParam("path") String path) throws Exception {
		// onopen called when client side websocket initiates connection to this
		// server.
		// path argument identifies which client page is on other end
		// for 'landing' page connections add the ChatAnnotation object to
		// static list.
		System.out.println("Opened Websocket @ /"+path+" by sID-"+session.getId());
		//System.out.println("path: " + path);
		
		this.session = session;
		//System.out.println("Current sessionID: " + session.getId());
		
		switch (path) {
		case "landing":
			// Do nothing
			return;
		case "admin":
		case "login": //fall through- they both do the same thing
			//Send information about groups (num, exists) so the admin/login page can inform
			//the user and act properly
			sendXMLCheckGroups(session.getId());
		
		//FALL THROUGH	
		case "chat": //chat doesn't need group information, only the connectedSessions
			synchronized (connectedSessions) {
				connectedSessions.add(this.session);
			}
			//connections.add(this);
			return;
		default:
			return;

		}

	}

	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {
		System.out.println("Closing WebSocket @ /"+path+" by sID-"+session.getId());
		//System.out.println(" sessionID-"+session.getId());
		//connections.remove(this);
		synchronized (connectedSessions) {
			connectedSessions.remove(this.session);
		}
		
		if (!session.isOpen()) {
			System.out.print("User closed browser ");
			if (userClient!=null) {
				System.out.println("-Removing user "+userClient.IDString());
				userList.remove(userClient);
			} else
				System.out.println("User not found");
			return;
		}
		if (path.equals("chat")) {
			if(userClient==null) return; //User doesnt have a clientID, shouldnt be on page
			
			System.out.println("Disconnecting permID: "+userClient.IDString() +" -> group "+userClient.groupID);

			if(userClient.groupID==0) return; //invalid user using an old ID from refreshing
			int bGroupID = userClient.groupID; //save groupID before removing from group

			Set<Client> group = groupManager.getGroup(userClient.groupID);
			userClient.groupID = 0; //0 not in a group
			group.remove(userClient);
			
			
			//Tell users about successful exit
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element exitMsg = doc.createElement("message");
			exitMsg.setAttribute("type", "alert");
			exitMsg.setAttribute("senderID", "Wooz2");
			exitMsg.setAttribute("groupNumber", Integer.toString(bGroupID));
			exitMsg.setAttribute("senderColor", userClient.chatColor.toString());
			exitMsg.setAttribute("senderName", userClient.username);
			exitMsg.appendChild(doc.createElement("text"));
			exitMsg.getFirstChild().setTextContent(userClient.username+" has left the group.");
			
			if (group.size()==0) {
				//need to log the room exit, but not broadcast cause group is empty
				//long dateTimestamp = new Date().getTime();
				System.out.println("Group "+bGroupID+" is empty");
				exitMsg.setAttribute("timestamp", serverDateFormatter.format(new Date()));
				synchronized (fileLogger.getLoggedClientMessages()) {
					fileLogger.captureMessage(exitMsg);
				}
				System.out.println("-Disconnect for "+userClient.IDString()+" not broadcast to empty group");
				//return;
			} else {
				broadcastGroup(exitMsg, bGroupID);
				sendGroupMembers(bGroupID); //update group members
				//return;
			}
		}
		 
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
		Document doc = builder.parse(is); //& crashes here *fixed client side

		// get message type
		Element element = doc.getDocumentElement();
		String messageType = element.getAttribute("type");
		
		System.out.println("Recieved: "+convertXMLtoString(element));

		String senderID = null; 
		
		if (messageType.equals(MessageType.Landing_SetUserType)) {
			//From Landing page- Set user type based on client input
			senderID = session.getId();
			
			//Set the permanent ID of the client here; add to number of recent users
			this.userClient = new Client(senderID);
			System.out.println("SET permID: "+userClient.permID+" -> "+ userClient.toString());
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
			//PermID causes issues where IE? asks for a new ID but uses an old ID
			//Removing until further notice
			/*case "PermIDGet":
				//Work around for login page
				System.out.println("Login page requesting permanent ID");
				sendXMLMessage("permID", userClient.getPermID());
				return;
				*/
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
			//Read-only operation
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
		
		//Dummy message to set the userClient on connect
		if (messageType.equals(MessageType.UserClientSet)) {
			System.out.println("Confirmed userClient-"+userClient.IDString());
			return;
		}
		
		if (messageType.equals(MessageType.Admin_GroupCreation)) {
			//From ADMIN page, called when accessing group creation
			
			int numGroups = Integer.parseInt(element.getTextContent());
			int groupOffset = Integer.parseInt(element.getAttribute("groupOffset"));
			String instructor = element.getAttribute("instructor");
			
			if(numGroups == 0) return; //Ignore invalid messages
			
			groupManager = new GroupManager(numGroups, instructor, groupOffset);
			
			//End Filelogger and recreate new one
			if (fileLogger!=null) fileLogger.destroy();
			fileLogger = new FileLogger(groupManager, ++groupIteration, logPath);
			
			adminCreatedGroups = true;
			
			broadcastCheckGroups(); //Broadcast change of groups to all users
			sendXMLMessage("groupsCreated", userClient.permID); //just additional confirmation for admin page
			return;
			
		} else if (messageType.equals(MessageType.Login_JoinGroup)) {
			//FROM LOGIN PAGE, add user to group
			//Init the userClient to add to group, but dont add the userClient to the group until they
			//connect through the chat page. Because if a broadcast is sent to the old Session, it will disconnect
			//System.out.println("joinGroup message received");
			//System.out.println("permID: "+userClient.permID+" -> "+ userClient.toString());
			
			int groupNumber = Integer.parseInt(element.getTextContent());
			System.out.println("Join group message: PermID "+userClient.IDString()+" -> ("+groupNumber+")");

			//If no groups, send alert to tell user
			if (!adminCreatedGroups) {
				sendXMLMessage("alert", userClient.permID,
						"No groups have currently been created. Please wait until an Admin creates groups to join.");
				System.out.println("joinGroup failed: No groups exist.");
				return;
			}
			
			//Test for invalid group Number
			if(groupNumber<groupManager.groupOffset || groupNumber>groupManager.groupOffset+groupManager.getNumOfGroups()) {
				sendXMLMessage("alert", userClient.permID, "Group ID is valid, sent "+groupNumber);
			}
			
			//If successful, initialize groupMember info, then send them to chat page
			userClient.username = element.getAttribute("username");
			userClient.groupID = groupNumber;
			groupManager.assignChatColor(userClient);
			
			System.out.println("User: "+userClient.IDString()+"-> Group: "+userClient.groupID+"-> color: "+userClient.chatColor.toString());
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
				//sendXMLMessage("alert", "Wooz2", "Problem with ID, please \n close the browser, refresh and \n go back to the landing page.");
				System.out.println("User refreshed page, exiting");
				sendXMLRedirect("login", userClient.permID);
				return;
			}
			//New user joining the chat room of group X
			//System.out.println("New user joining the chatroom!");
			//All information is already in the userClient
			System.out.println("currID: "+userClient.sessionID+" -> User "+userClient.IDString()+" -> joinChat->Group "+userClient.groupID);
			//System.out.println(userClient.toString()+" -> Group "+userClient.groupID);
			
			//Must join group after websocket Chat is open, otherwise disconnect can occur
			boolean joinGroupErr;
			joinGroupErr = groupManager.joinGroup(userClient.groupID, userClient); //cant add duplicates
			if (!joinGroupErr) {
				sendXMLMessage("alert", userClient.permID, 
						"Could not join Group "+userClient.groupID+". Please ask for assistance.");
				return;
			}
			
			//Instead of making a literal text string, we will create an Element because
			//its a little easier to add things to it programmatically
			Element broadMsg = doc.createElement("message");
			broadMsg.setAttribute("type", "alert");
			broadMsg.setAttribute("senderID", "Wooz2"); //Server name
			broadMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			//sending senderColor/name for client side beautification
			broadMsg.setAttribute("senderColor", userClient.chatColor.toString());
			broadMsg.setAttribute("senderName", userClient.username); //Not needed for alert messages
			broadMsg.appendChild(doc.createElement("text"));
			broadMsg.getFirstChild().setTextContent(userClient.username +" has joined the group.");
			
			//System.out.println("Broadcasting user join message via alert");
			System.out.println("User "+userClient.IDString()+" joined chat for Group "+userClient.groupID);

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
				//user has technically disconnected from chat? Some how?
				sendXMLMessage("alert", "Wooz2", "Problem with ID, please \n close the browser, refresh and \n go back to the landing page.");
				return;
			}
			//Add groupMember information to element - senderID is already there
			//Could add this stuff in broadcast method... but interferes with alert chat msgType...
			//Timestamp will be added in broadcastGroup method
			element.setAttribute("senderName", userClient.username);
			element.setAttribute("senderColor", userClient.chatColor.toString());
			element.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			element.setAttribute("sessionID", userClient.sessionID);
			
			//System.out.println("Broadcasting Chat/Typing Message: "+convertXMLtoString(element));
			broadcastGroup(element, userClient.groupID);
			return;
		}
		//END OF REFACTORING
		
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
			System.out.println("Invalid groupID = 0");
			session.close();
			return; //There's an issue where refreshing the page removes the user from the group
			//but only after a chat message, should be resolved now
		}
		
		//Capture message in FileLogger here
		//long dateTimestamp = new Date().getTime();
		String dateTimestamp = serverDateFormatter.format(new Date());
		//String dateTimestamp = serverDateFormatter.format(new Date());
		//DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
		//dateTimestamp = df.format(new Date());
		
		//Time in milliseconds is the easiest to translate in JS without losing important information
		msg.setAttribute("timestamp", dateTimestamp);
		synchronized (fileLogger.getLoggedClientMessages()) {
			fileLogger.captureMessage(msg);
		}
		
		System.out.println("Broadcast Message: "+convertXMLtoString(msg));
		
		Set<Client> group = groupManager.getGroup(groupID);
		
		//Get each client and push message to them
		String xmlStr = convertXMLtoString(msg);
		//synchronized (group) {
		System.out.print("Broadcast to group ("+groupID+ ") : ");
		for (Client gClient : group) {
			try {
				synchronized (gClient) { //Possible issue where two people type at the same time can cause dc
					gClient.session.getBasicRemote().sendText(xmlStr);
					System.out.print(gClient.IDString()+" : ");
				}
			//a ton of error handling for lost messages
			} catch (Exception e) {
				log.debug("Chat Error: Failed to send message to client "+gClient.permID, e);
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
				disconnectMsg.setAttribute("senderID", "Wooz2"); //send server name
				disconnectMsg.setAttribute("groupNumber", Integer.toString(groupID));
				//disconnectMsg.setAttribute("senderColor", GroupManager.SERVER_COLOR.toString());
				disconnectMsg.appendChild(doc.createElement("text"));
				disconnectMsg.getFirstChild().setTextContent(gClient.username + " has disconnected from the group.");
				broadcastGroup(disconnectMsg, groupID);

			}//end of try catch		
		}//end of for - for each group Member
		System.out.println();
		//}//end of synchronized group
		
		
	} //End of broadcastGroup
		
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
		int groupOffset = adminCreatedGroups ? groupManager.groupOffset : 0;
		System.out.println("Sent Group Info - num: "+groups+" to sID-"+session.getId());
		String msg = "<message type='checkGroups' groupOffset='"+groupOffset+"' checkGroups='" + groups + "' iteration='"+groupIteration+"' senderID='" + senderID +"'></message>";
		session.getBasicRemote().sendText(msg);
	}
	
	//Send to all clients currently connected to server
	private void broadcastCheckGroups() throws IOException {
		String groups = adminCreatedGroups ? Integer.toString(groupManager.getNumOfGroups()) : "Not Created";
		int groupOffset = adminCreatedGroups ? groupManager.groupOffset : 0;
		String msg = "<message type='checkGroups' groupOffset='"+groupOffset+"' checkGroups='" + groups + "' senderID='Wooz2 Broadcast'></message>";
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
		
		//So for future updates, we want to get the last typing event if that's the last thing
		//a user has contributed. So we would make an array based on the list of groupMembers,
		//populate with their messages and take the last message if its not an empty typing/chat.
		//Read-only operation
		for (int i=0; i<chatHis.size(); i++) {
			Element chatMsg = (Element) chatHis.get(i);
			if (userClient.groupID != Integer.parseInt(chatMsg.getAttribute("groupNumber")) 
					|| chatMsg.getAttribute("type").equals("typing"))  //get everything thats not typing
				continue;
			chatMsg.setAttribute("chatHistory", "chatHistory"); //add extra value for fancy purposes
			
			session.getBasicRemote().sendText(convertXMLtoString(chatMsg));
		}
		System.out.println("Chat history sent to user "+userClient.IDString());
	}
	
	private void sendGroupMembers(int groupID) throws Exception {
		//This sends a list of group members NAMES and colors
		//For UI purposes only
		System.out.println("Sending list of group members for group "+groupID);
		//Cant use an attribute so nodes will do
		String msg = "<message type='lGroupMembers' groupNumber='"+groupID+"'>";
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
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0)
	  {
	    System.out.println("Destroying server");
	    if(userCullTimer!=null)
	    	userCullTimer.cancel();
	    if(fileLogger!=null)
	    	fileLogger.destroy(); //save files on server exit
	  }
	  
	@Override
	  public void contextInitialized(ServletContextEvent sce)
	  {
	    System.out.println("Init server"); //called when server context is made
	    
	    String initLogPath = sce.getServletContext().getInitParameter("logPath");
	    //iternary operator; fancy way of saying use default if initLogPath is empty
	    String OS = System.getProperty("os.name");
	    System.out.println("Running on "+OS+" machine.");
	    if (!OS.contains("Windows"))
	    	logPath = (initLogPath == null) || (initLogPath.equals("")) ? logPath : initLogPath;
	    System.out.println("logPath initialized to: " + logPath);
	    
	    System.out.println("Activating userClient Culler");
	    userCullTimer = new Timer();
	    userCullTimer.schedule(new UserClientCullerClass(this.userList), UserClientCullerClass.userCullTime,
	    		UserClientCullerClass.userCullTime);
	  }
}

//Removes unused userClients to prevent issues with 
class UserClientCullerClass extends TimerTask {
	
	public ArrayList<Client> userList;
	public static int userCullTime = 1000*60*5;
	public static int userReconnectTime = 1000*60*2;
	
	public UserClientCullerClass (ArrayList<Client> userL) {
		userList = userL;
	}
	
	public void run () {
		//System.out.println("Running userList cull method");
		ArrayList<Client> cullList = new ArrayList<Client>();
		for(int i=0; i<userList.size(); i++) {
			Client user = userList.get(i);
			if (!user.session.isOpen()) { //If userClient is disconnected
				System.out.println("Observing user "+user.IDString()+" for inactivity");
				cullList.add(user);
			}
		} //end of for loop
		try {
			Thread.sleep(userReconnectTime); //Give the user approx. 2 mins to reconnect
		} catch (Exception e) {
			System.out.println("Error occurred while sleeping in userCull Class");
			System.out.println(e.getMessage());
			return;
		}
		for(int i=0; i<cullList.size(); i++) {
			Client user = cullList.get(i);
			if (!user.session.isOpen()) {//If userClient is still disconnected after 2 mins
				userList.remove(user);	//Remove client from server registry
				System.out.println("Dropping userClient "+user.IDString()+ " from registry.");
			}
		}
	}
	
}
