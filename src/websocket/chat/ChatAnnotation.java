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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import util.GroupInfoObject;
import util.MessageType;

@WebListener
@ServerEndpoint(value = "/{path}")
public class ChatAnnotation implements ServletContextListener{

	private static final Log log = LogFactory.getLog(ChatAnnotation.class);

	//private static final String GUEST_PREFIX = "Guest";
	private static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	private static final DateFormat timeDifferenceFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
	private static final Date serverStartTime = new Date();
	
	//private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<>();
	private static final ArrayList<Session> connectedSessions = new ArrayList<Session>();
	private static final ArrayList<Client> userList = new ArrayList<Client>();
	
	public static FileLogger fileLogger;// = new FileLogger();
	public static GroupManager groupManager; //note- made this public for FileLogger to delete reference
	private static Timer userCullTimer;

	//private static boolean adminCreatedGroups = false;

	private static int groupIteration = 0;
	//private static int uniqueServerID = new java.util.Random(new Date().getTime()).nextInt();
	private static String logPath = "log\\";
	
	//private String nickname;
	private Session session;
	private Client userClient;
	private Integer[] AMLineCounter;

	public ChatAnnotation() {
		// a new ChatAnnotation object is created for every time a WEBSOCKET that
		// connects to server.
		//System.out.println("Chat Annotation Constructor");
	}

	@OnOpen
	public void start(Session session, @PathParam("path") String path) throws Exception {
		// onopen called when client side websocket initiates connection to this server.
		// path argument identifies which client page is on other end
		System.out.println("Opened Websocket @ /"+path+" by sID-"+session.getId());
		
		this.session = session;
		//WATCH OUT FOR FALL THROUGH
		switch (path) {
		case "admin":
			sendXMLGroupSetInfo(session);
			break;

		case "login": 
		case "chat": //outdated now
			break;
			
		case "adminMonitor":
		case "loginChat":
			sendXMLGroupInfo(session);
		
			//session.setMaxIdleTimeout(1000*60);
			//System.out.println("Set timeout to: "+session.getMaxIdleTimeout());
			
			synchronized (connectedSessions) {
				connectedSessions.add(this.session);
			}
			//System.out.println("path "+session.getPathParameters().toString());
			return;
		default:
			return;

		}

	}

	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {
		System.out.println("Closing WebSocket @ /"+path+" by sID-"+session.getId());
		
		synchronized (connectedSessions) {
			connectedSessions.remove(this.session);
		}
		
		//Session has already been closed before end could run
		//Does happen with wi-fi clients (?)
		if (!session.isOpen()) {
			System.out.print("Session was closed- ");
			if (userClient!=null) {
				System.out.println("Removing user "+userClient.IDString()+" due to closed session");
				//userList.remove(userClient); //Monitoring this
				//Continue processing if user was on chat page
			} else {
				System.out.println("User not found");
				return; //do not tell group about disconnect
			}
		}
		
		//Exit processing for chat
		if (path.equals("chat")) {
			//Either the user sent the leaveChat msg groupID = 0, or they did not groupID = #
			if(userClient==null) {  //User doesnt have a clientID, shouldnt be on page
				System.out.println("Invalid user on the chat page- sID"+session.getId());
				return; //Util.getPermID() should validate this
			} else if (userClient.groupID==-1) {
				System.out.println("User skipped login validation "+userClient.IDString());
				return; //Incoming->joinChat-> checkGroup should redirect them already
			} else if (userClient.groupID==0) {
				System.out.println("User "+userClient.IDString()+" succesfully left chat page- Broadcast sent via incoming");
				return; //Incoming->leaveChat has already processed this
			}
			
			//Otherwise this is a user that did not send leaveChat
			System.out.println("Disconnecting user: "+userClient.IDString() +" -> group "+userClient.groupID);

			int bGroupID = userClient.groupID; //save groupID before removing from group

			Set<Client> group = groupManager.getGroup(userClient.groupID);
			//userClient.groupID = 0;
			group.remove(userClient);
			
			
			//Tell users about unsuccessful disconnect
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
			exitMsg.getFirstChild().setTextContent(userClient.username+" has disconnected from the group. (End)");
			
			if (group.size()==0) { //need to log the room exit, but not broadcast cause group is empty
				exitMsg.setAttribute("timestamp", serverDateFormatter.format(new Date()));
				synchronized (fileLogger.getLoggedClientMessages()) {
					fileLogger.captureMessage(exitMsg);
				}
				System.out.println("Disconnect for "+userClient.IDString()+" not broadcast to empty group.  (End)");
			} else {
				broadcastGroup(exitMsg, bGroupID);
				sendGroupMembers(bGroupID); //update group members
			}
		}
	}

	@OnMessage
	public void incoming(String message, @PathParam("path") String path) throws Exception {
		// parse xml message and send broadcast to group.
		// Handles different types of messages
		
		// parse xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		StringReader sr = new StringReader(message);
		InputSource is = new InputSource(sr);
		Document doc = builder.parse(is); //Invalid XML will crash here

		// get message type
		Element element = doc.getDocumentElement();
		String messageType = element.getAttribute("type");
		
		System.out.println("Recieved: "+convertXMLtoString(element));

		String senderID = null; 
		
		//Get the senderID and Client and link to this ChatAnnotation class
		senderID = element.getAttribute("senderID");
		
		if(messageType.equals(MessageType.UserClientAffirm)) {
			//If a clientID is included, confirm it the userClient and send it
			//Else create a new userClient
			
			//Use senderID to find userClient and link userClient
			if (senderID!="") {
				int userListSize = userList.size(); //set before for is run
				for (int i=0; i<userListSize; i++) {
					Client user = userList.get(i);
					if (user.permID.equals(senderID)) {
						this.userClient = user;
						System.out.println("CONFIRM permID: "+userClient.permID+" -> "+ userClient.toString());
						sendXMLMessage("permIDConfirm", userClient.permID);
						break;
					}
				}
			}
			
			//if no userClient was found (and by default senderID is empty)
			if (userClient==null) {
				if (senderID!="") {
					System.out.print("Outdated PermID-"+senderID+" ");
				}
				this.userClient = new Client(session.getId(), new SimpleDateFormat("-HHmmssSS").format(serverStartTime) );
				//senderID = userClient.permID;
				System.out.println("AFFIRM permID: "+userClient.permID+" -> "+ userClient.toString());
				userList.add(this.userClient);
				sendXMLMessage("permIDSet", userClient.permID);
			}
			
			userClient.sessionID = this.session.getId();
			userClient.session = this.session;
			return;
		}
		
		//userClient AND senderID MUST/ARE be set after this point
		if (messageType.equals(MessageType.Admin_GroupCreation)) {
			//From ADMIN page, called when accessing group creation
			
			int numGroups = Integer.parseInt(element.getTextContent());
			int groupOffset = Integer.parseInt(element.getAttribute("groupOffset"));
			String logName = element.getAttribute("logName");
			
			if(numGroups == 0 || groupOffset < 0) return; //Ignore invalid messages
			
			if (groupManager!=null) groupManager.destroy();
			groupManager = new GroupManager(numGroups, groupOffset, logName);
			
			//End Filelogger and recreate new one
			if (fileLogger!=null) fileLogger.destroy();
			fileLogger = new FileLogger(groupManager, ++groupIteration, logPath);
			
			//adminCreatedGroups = true;
			
			//broadcastCheckGroups(); //Broadcast change of groups to all users
			for (int i=0; i<connectedSessions.size(); i++) {
				sendXMLGroupInfo(connectedSessions.get(i));
			}
			sendXMLGroupSetInfo(this.session);
			//sendXMLMessage("groupsCreated", userClient.permID); //just additional confirmation for admin page
			return;
			
		} else if (messageType.equals(MessageType.Admin_GroupDeletion)) {
			//Destroy the current groupManager and fileLogger
			if (groupManager!=null) groupManager.destroy();
			groupManager = null;
			if (fileLogger!=null) fileLogger.destroy();
			fileLogger = null;
			//adminCreatedGroups = false;
			
			//update all clients
			for (int i=0; i<connectedSessions.size(); i++) {
				sendXMLGroupInfo(connectedSessions.get(i));
			}
			sendXMLGroupSetInfo(this.session);
			
			return;
		}
		
		//Here we'll place Admin Monitor functions. AM needs work around to get around certain 
		//aspects of how chat & groups work; while keeping all the information to make AM useful but anonymous
		//This means AM will supply its group information- but also receive info from groups 
		else if (messageType.equals(MessageType.AdminMonitor_Login)) {
			//Logging in as an Admin Monitor- set up admin functions to this userClient
			String username = element.getTextContent();
			userClient.username = username;
			
			if (userClient.isAdmin) {
				System.out.println("User: "+userClient.IDString()+" is already an admin");
				sendAMStatus(groupManager.getAMStatus(userClient));
				return;
			}
			
			groupManager.addAdminMonitor(userClient);
			sendAMStatus(groupManager.getAMStatus(userClient));
			
			System.out.println("User: "+userClient.IDString()+" on sID-"+session.getId()+" is an Admin Monitor");
			return;
		} else if (messageType.equals(MessageType.AdminMonitor_Status)) {
			//Client updating its admin Monitor info
			if (!userClient.isAdmin) {
				this.sendXMLMessage("alertWebPage", senderID, "User is not an Admin- Please Login");
				return;
			}
			
			//Chat/Group login & logout are handled here- 
			int[] serverAMStatus = groupManager.getAMStatus(userClient);
			int[] clientAMStatus = serverAMStatus.clone(); //deep clone hopefully
			
			int groupID = Integer.parseInt(element.getAttribute("groupID"));
			clientAMStatus[groupManager.getGroupNo(groupID)] = Integer.parseInt(element.getTextContent());
				
			//Send info so client can prepare the chat window
			groupManager.setAMStatus(userClient, clientAMStatus); //serverStatus popped off here
			sendAMStatus(clientAMStatus);
			
			/**************************************************
			 * Start processing based on changed state
			 **************************************************/
			int groupNo = groupManager.getGroupNo(groupID);
			int currAMStat = clientAMStatus[groupNo];
			int pastAMStat = serverAMStatus[groupNo];
			if (currAMStat == pastAMStat) return;
			if (pastAMStat==GroupManager.AM_NONE) { //Going into group
				userClient.groupID = groupID;
				groupManager.assignChatColor(userClient);
				sendChatHistory(userClient, 0, false);
				groupManager.joinGroup(groupID, userClient);
					
				System.out.print("User AM "+userClient.IDString()+" -> Group "+groupID);
				
			} else if (currAMStat==GroupManager.AM_NONE) { //leaving group
				
				int bGroupID = groupID;
				Set<Client> group = groupManager.getGroup(bGroupID);
				if (group==null)
					return;
				group.remove(userClient);
				System.out.println("User AM: "+userClient.IDString() +" -> Left Group "+groupID);
			}
			
			if (pastAMStat==GroupManager.AM_CHAT) //if leaving chat
				sendExitMessage(doc, userClient, groupID);
			else if (currAMStat==GroupManager.AM_CHAT) { //send chat connect msg
				userClient.groupID = groupID;
				sendEnterMessage(doc, userClient);
			}
			
			System.out.println("AM User: "+userClient.IDString()+" has updated its AM Status");
			
		//Standard functions for Student/Login/Chat users
		} else if (messageType.equals(MessageType.Login_JoinGroup)) {
			//FROM LOGIN PAGE, add user to group
			
			int groupNumber = Integer.parseInt(element.getTextContent());
			System.out.println("Join group message: PermID "+userClient.IDString()+" -> ("+groupNumber+")");

			//If no groups, send alert to tell user
			if (fileLogger==null) {
				sendXMLMessage("webAlert", userClient.permID,
						"No groups have currently been created. Please wait until an Admin creates groups to join.");
				return;
			}	//Test for invalid group Number
			else if(groupNumber<=groupManager.groupOffset || groupNumber>groupManager.groupOffset+groupManager.groupTotal) {
				sendXMLMessage("webAlert", userClient.permID, "Group ID is invalid, sent "+groupNumber);
				return;
			}
			
			//If successful, initialize groupMember info, then send them to chat page
			userClient.username = element.getAttribute("username");
			userClient.groupID = groupNumber;
			groupManager.assignChatColor(userClient);
			
			System.out.println("User: "+userClient.IDString()+"-> Group: "+userClient.groupID+"-> color: "+userClient.chatColor.toString());
			
			boolean	joinGroupSuccess = groupManager.joinGroup(userClient.groupID, userClient);
			if (!joinGroupSuccess) {
				sendXMLMessage("webAlert", userClient.permID, "Could not join Group. Please contact an admin.");
				return;
			}
			
			sendXMLMessage("displayChat", userClient.permID); //for loginChat.html to display chat
			
			sendChatHistory(userClient, 0, true);
			sendGroupAnswerStatus(userClient.groupID);
			sendEnterMessage(doc, userClient);
			
			/*Element broadMsg = doc.createElement("message");
			broadMsg.setAttribute("type", "alert");
			broadMsg.setAttribute("senderID", "Wooz2"); //Server name
			broadMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			//sending senderColor/name for client side beautification
			broadMsg.setAttribute("senderColor", userClient.chatColor.toString());
			broadMsg.setAttribute("senderName", userClient.username);
			broadMsg.appendChild(doc.createElement("text"));
			broadMsg.getFirstChild().setTextContent(userClient.username +" has joined the group.");
			
			sendChatHistory(userClient, 0, true);
			broadcastGroup(broadMsg, userClient.groupID);
			sendGroupMembers(userClient.groupID);
			*/
			return;
			
		} else if (messageType.equals(MessageType.Chat_JoinChat)||messageType.equals(MessageType.Chat_ReconnectChat)) {
			
		} else if (messageType.equals(MessageType.Chat_LeaveChat)) {
			//Graceful exit from chat websocket
			if (userClient==null) {
				System.out.println("Session "+session.getId()+" does not have a valid userClient, ignoring");
				return; //User doesnt have a clientID, shouldnt be on page
			} else if (userClient.groupID==0) { //0 means user has not joined a group
				System.out.println(userClient.getPermID()+" has not joined a group.");
				return;
			} else if (groupManager==null) {
				System.out.println("There are no groups created to be left.");
				return;
			}
			
			System.out.println("Leaving chat: PermID: "+userClient.IDString() +" -> group "+userClient.groupID);

			int bGroupID = userClient.groupID; //save groupID before removing from group

			Set<Client> group = groupManager.getGroup(bGroupID);
			userClient.groupID = 0; //0 not in a group
			if (group==null) //group did not exist
				return;
			group.remove(userClient);
			
			sendExitMessage(doc, userClient, bGroupID);
			/*
			//Tell users about exit from chat
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
				exitMsg.setAttribute("timestamp", serverDateFormatter.format(new Date()));
				synchronized (fileLogger.getLoggedClientMessages()) {
					fileLogger.captureMessage(exitMsg);
				}
				System.out.println(" Leave chat for "+userClient.IDString()+" not broadcast to empty group");
			} else {
				broadcastGroup(exitMsg, bGroupID);
				sendGroupMembers(bGroupID); //update group members
			}
			*/
			//userClient.session.close(); //Attempt to close connection from server side
		 
		} else if (messageType.equals(MessageType.Chat_Typing) || messageType.equals(MessageType.Chat_Chat)) {
			//From Chat- messages in the chat room
			
			if(userClient.groupID==0 || userClient.groupID==-1) {
				//user has technically disconnected from chat? Some how?
				sendXMLMessage("alert", "Wooz2", "Problem with ID, please \n close the browser, refresh and \n go back to the landing page.");
				return;
			}
			//Add groupMember information to element - senderID is already there
			//Timestamp will be added in broadcastGroup method
			element.setAttribute("senderName", userClient.username);
			element.setAttribute("senderColor", userClient.chatColor.toString());
			if (element.getAttribute("groupNumber").isEmpty()) //adminMonitor provides groupNumber and is not bound to its userClient
				element.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			element.setAttribute("sessionID", userClient.sessionID);
			
			//In order to work with adminMonitors, we need to parse the groupNumber from the groupNumber attribute
			int groupSend = Integer.parseInt(element.getAttribute("groupNumber"));
			broadcastGroup(element, groupSend);
			return;
		} else if (messageType.equals(MessageType.Answer_Type)) {
			//Send message to group like typing event
			//int groupID = Integer.parseInt(element.getAttribute("groupNumber"));
			if (groupManager.getAnswerLock(userClient.groupID)) {
				System.out.println("Answer is locked-Group "+userClient.groupID);
				return;
			}
			groupManager.setAnswer(userClient.groupID, element.getTextContent());
			
			element.setAttribute("senderName", userClient.username);
			element.setAttribute("senderColor", userClient.chatColor.toString());
			element.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			//element.setAttribute("sessionID", userClient.sessionID);
			
			broadcastGroup(element, userClient.groupID);
			return;
		} else if (messageType.equals(MessageType.Answer_Status)) {
			userClient.answerStatus = Boolean.parseBoolean(element.getAttribute("status"));
			System.out.println("User "+userClient.IDString()+" changed status to "+userClient.answerStatus);
			
			//send message based on answerLock & status
			Element ansMsg = doc.createElement("message"); 
			ansMsg.setAttribute("type", "alert");
			ansMsg.setAttribute("senderName", userClient.username);
			ansMsg.setAttribute("senderID", userClient.permID);
			ansMsg.setAttribute("senderColor", userClient.chatColor.toString());
			ansMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
			ansMsg.appendChild(doc.createElement("text"));
			if (!groupManager.getAnswerLock(userClient.groupID)) {
				if (userClient.answerStatus)
					ansMsg.getFirstChild().setTextContent(userClient.username+" is ready to submit the answer!");
				else 
					ansMsg.getFirstChild().setTextContent(userClient.username+" has cancelled their submission.");
			} else {
				if (userClient.answerStatus)
					ansMsg.getFirstChild().setTextContent(userClient.username+" would like to withdraw the submission!");
				else 
					ansMsg.getFirstChild().setTextContent(userClient.username+" would like to continue with the submission.");
			}
			
			broadcastGroup(ansMsg, userClient.groupID);
			sendGroupAnswerStatus(userClient.groupID); //send full update on answerStatus
			return;
			
		} else if (messageType.equals(MessageType.Answer_UnderReview)) {
			int groupID = Integer.parseInt(element.getAttribute("groupNumber")); 
			
			Element ansMsg = doc.createElement("message"); 
			ansMsg.setAttribute("type", "alert");
			ansMsg.setAttribute("senderID", userClient.permID);
			ansMsg.setAttribute("senderName", userClient.username);
			ansMsg.setAttribute("senderColor", userClient.chatColor.toString());
			ansMsg.setAttribute("groupNumber", Integer.toString(groupID));
			ansMsg.appendChild(doc.createElement("text"));
			ansMsg.getFirstChild().setTextContent(userClient.username+" is currently reviewing your answer!");
			
			broadcastGroup(ansMsg, groupID);
			return;
		} else if (messageType.equals(MessageType.Answer_Review)) {
			int groupID = Integer.parseInt(element.getAttribute("groupNumber"));
			String answerReview = element.getAttribute("answerReview");
			
			Set<Client> group = groupManager.getGroup(groupID);
			for (Client c : group) {
				c.answerStatus = false;
			}
			
			int groupNo = groupManager.getGroupNo(groupID);
			groupManager.answerLock[groupNo] = false;
			groupManager.prevAnswer[groupNo] = groupManager.answer[groupNo]; 
			

			Element ansMsg = doc.createElement("message"); 
			ansMsg.setAttribute("type", "alert");
			ansMsg.setAttribute("senderID", userClient.permID);
			ansMsg.setAttribute("senderName", userClient.username);
			ansMsg.setAttribute("senderColor", userClient.chatColor.toString());
			ansMsg.setAttribute("groupNumber", Integer.toString(groupID));
			ansMsg.appendChild(doc.createElement("text"));
			ansMsg.getFirstChild().setTextContent(userClient.username+" has marked your answer as "+answerReview+"!");
			
			sendGroupAnswerStatus(groupID);
			broadcastGroup(ansMsg, groupID);
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
		
		/*if (groupID==0) {
			System.out.println("Invalid groupID = 0");
			session.close();
			return; //There's an issue where refreshing the page removes the user from the group
			//but only after a chat message, should be resolved now
		}*/
		
		//Capture message in FileLogger here
		String dateTimestamp = serverDateFormatter.format(new Date());
		msg.setAttribute("timestamp", dateTimestamp);
		synchronized (fileLogger.getLoggedClientMessages()) {
			fileLogger.captureMessage(msg);
		}
		
		System.out.println("Broadcast Message: "+convertXMLtoString(msg));
		
		Set<Client> group = groupManager.getGroup(groupID);
		
		if (group.size()==0) {
			System.out.println("Message not broadcast to empty group.");
			return;
		}
		
		//Get each client and push message to them
		String xmlStr = convertXMLtoString(msg);
		System.out.print("Broadcast to group ("+groupID+ ") : ");
		for (Client gClient : group) {
			try {
				//It should throw a IllegalStateException should this fail
				synchronized (gClient.session) {
				if (gClient.session.isOpen()) {
					gClient.session.getBasicRemote().sendText(xmlStr); 
					System.out.print(gClient.IDString()+" : ");
				} else
					throw new Exception();
				}
			//a ton of error handling for lost messages
			} catch (Exception e) {
				log.debug("Chat Error: Failed to send message to client "+gClient.permID, e);
				System.out.println("Chat Error: Failed to send message to client "+gClient.IDString()+" "+e.getMessage());
				group.remove(gClient);
				try {
					gClient.session.close();
				} catch (Exception ex) {
					log.error("Server Error: Could not disconnect client");
					System.out.println("Server Error: Could not disconnect client");
				}
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.newDocument();
				Element disconnectMsg = doc.createElement("message");
				
				disconnectMsg.setAttribute("type", "alert");
				disconnectMsg.setAttribute("senderID", "Wooz2"); //send server name
				disconnectMsg.setAttribute("groupNumber", Integer.toString(groupID));
				disconnectMsg.setAttribute("senderColor", gClient.chatColor.toString());
				disconnectMsg.appendChild(doc.createElement("text"));
				disconnectMsg.getFirstChild().setTextContent(gClient.username + " has disconnected from the group. (Msg)");
				broadcastGroup(disconnectMsg, groupID);

			}//end of try catch		
		}//end of for - for each group Member
		System.out.println();
		
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
	
	//Message about server and group info.
	private void sendXMLGroupSetInfo (Session sendSession) throws Exception {
		
		Element e = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder().newDocument().createElement("message");
		
		e.setAttribute("type", "groupSetInfo");
		
		if (groupManager==null) {
			e.setAttribute("setStatus", "FALSE");
			e.setAttribute("groupOffset", "#");
			e.setAttribute("groupTotal", "#");
			e.setAttribute("setStartTime", "##:##:##:###");
			e.setAttribute("logName", "NO DATA");
		} else {
			e.setAttribute("setStatus", "TRUE");
			e.setAttribute("groupOffset", Integer.toString(groupManager.groupOffset));
			e.setAttribute("groupTotal", Integer.toString(groupManager.groupTotal));
			//Date createDiff = new Date(new Date().getTime() - groupManager.setCreateDate.getTime() ); 
			e.setAttribute("setStartTime", serverDateFormatter.format(groupManager.setCreateDate));
			e.setAttribute("logName", groupManager.logName);
		}
		
		if (fileLogger==null) {
			e.setAttribute("logSaveLast", "##:##:##:###");
		} else {
			if (fileLogger.endDate==null)
				e.setAttribute("logSaveLast", "Not Saved");
			else {
				e.setAttribute("logSaveLast", serverDateFormatter.format(fileLogger.endDate));
			}
		}
		
		e.setAttribute("serverStartTime", serverDateFormatter.format(serverStartTime));
		sendSession.getBasicRemote().sendText(convertXMLtoString(e));
	}
	
	private void sendXMLGroupInfo (Session sendSession) throws Exception {
		Element e = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument().createElement("message");

		e.setAttribute("type", "groupInfo");
		
		if (groupManager==null) {
			e.setAttribute("setStatus", "FALSE");
			e.setAttribute("groupOffset", "#");
			e.setAttribute("groupTotal", "#");
		} else {
			e.setAttribute("setStatus", "TRUE");
			e.setAttribute("groupOffset", Integer.toString(groupManager.groupOffset));
			e.setAttribute("groupTotal", Integer.toString(groupManager.groupTotal));
		}
		sendSession.getBasicRemote().sendText(convertXMLtoString(e));
	}
	
	private void sendGroupAnswerStatus(int groupID) throws Exception {
		Set<Client> group = groupManager.getGroup(groupID);
		if (group.size()==0) //shouldn't be called with no group members
			return;
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element e = doc.createElement("message");
		e.setAttribute("type", "AnswerGroupStatus");
		//e.setAttribute("answerLock", Boolean.toString(groupManager.answerLock[groupManager.getGroupNo(groupID)]));
		e.setAttribute("groupNumber", Integer.toString(groupID));
		
		Element ea = doc.createElement("answer");
		ea.setTextContent(groupManager.answer[groupManager.getGroupNo(groupID)]);
		e.appendChild(ea);
		
		Element epa = doc.createElement("prevAnswer");
		epa.setTextContent(groupManager.prevAnswer[groupManager.getGroupNo(groupID)]);
		e.appendChild(epa);
		
		boolean allTrue = true;
		//Search through all group members that are not admins
		for (Client c : group) {
			if (c.isAdmin)
				continue;
			if (!c.answerStatus)
				allTrue = false;
		}
		
		if (allTrue) {
			//invert
			groupManager.answerLock[groupManager.getGroupNo(groupID)] = 
					!groupManager.answerLock[groupManager.getGroupNo(groupID)];
			for (Client c : group) {
				//if admin, send answerAlert
				if (c.isAdmin && groupManager.answerLock[groupManager.getGroupNo(groupID)]) {
					String answerAlert = "<message type='answerAlert' groupNumber='"+groupID+"'/>";
					c.session.getBasicRemote().sendText(answerAlert);
				}
				c.answerStatus = false;
			}
			
			Element answerStatMsg = doc.createElement("message");
			answerStatMsg.setAttribute("type", "alert");
			answerStatMsg.setAttribute("senderID", "Wooz2"); //send server name
			answerStatMsg.setAttribute("groupNumber", Integer.toString(groupID));
			answerStatMsg.appendChild(doc.createElement("text"));
			if (groupManager.answerLock[groupManager.getGroupNo(groupID)])
				answerStatMsg.getFirstChild().setTextContent("The current Answer has been submitted and locked!");
			else 
				answerStatMsg.getFirstChild().setTextContent("The current Answer has been withdrawn and can be edited!");
			
			broadcastGroup(answerStatMsg, groupID);
		}
		
		//do this after answer check
		e.setAttribute("answerLock", Boolean.toString(groupManager.answerLock[groupManager.getGroupNo(groupID)]));
		for (Client c : group) {
			if (c.isAdmin)
				continue;
			Element ec = doc.createElement("member");
			ec.setAttribute("senderID", c.permID);
			ec.setTextContent(Boolean.toString(c.answerStatus));
			e.appendChild(ec);
		}
		
		for (Client c : group) 
			c.session.getBasicRemote().sendText(convertXMLtoString(e));
	}
	
	private void sendAMStatus(int[] AMStatus) throws Exception {
		//helper function to format AMStatus for client to process
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element e = doc.createElement("message");
		e.setAttribute("type", "AMStatus");
		e.setAttribute("senderName", userClient.username);
		e.setAttribute("senderID", userClient.permID);
		e.setAttribute("groupNum", Integer.toString(groupManager.groupTotal));
		e.setAttribute("groupOffset", Integer.toString(groupManager.groupOffset));
		
		for (int i=0; i<AMStatus.length; i++) {
			int groupID = groupManager.getGroupNo(i);
			Element er = doc.createElement("groupAMInfo");
			er.setAttribute("groupID", Integer.toString(groupID));
			er.setTextContent(Integer.toString(AMStatus[i]));
			e.appendChild(er);
		}
		
		//System.out.println(convertXMLtoString(e));
		synchronized (session) {
			session.getBasicRemote().sendText(convertXMLtoString(e));
		}
		System.out.println("Sent Updated AM Status to user "+userClient.IDString());
	}
	
	//XML to String
	public static String convertXMLtoString(Element node) throws Exception {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		String str = serializer.writeToString(node);
		return str;
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
	private int sendChatHistory(Client userClient, int startLog, boolean lock) throws Exception {
		//get the copy/list of chat elements from the group this client is joining
		//Read-only operation & iterator; Doesnt need to be synchronized
		ArrayList<Element> chatHis = fileLogger.getLoggedClientMessages();
		
		//loop through loggedMessages and send anything from the same group
		HashMap<String, Element> elemChat = new HashMap<String, Element>();
		
		//Read-only operation
		int chatHisSize = chatHis.size();
		for (int i=startLog; i<chatHisSize; i++) {
			Element chatMsg = (Element) chatHis.get(i).cloneNode(true); //Need to clone node otherwise reference will pick up chatHistory attrib
			if (userClient.groupID != Integer.parseInt(chatMsg.getAttribute("groupNumber")) )
			//		|| chatMsg.getAttribute("type").equals("typing"))  //get everything thats not typing
				continue;
			
			elemChat.put(chatMsg.getAttribute("senderID"), chatMsg); //save message in buffer
			
			if (chatMsg.getAttribute("type").equals("typing")) //dont broadcast typing
				continue;
			
			chatMsg.setAttribute("chatHistory", "chatHistory"); //add extra value for fancy purposes
			synchronized (session) {
				session.getBasicRemote().sendText(convertXMLtoString(chatMsg));
			}
			if (lock)
				chatHisSize = chatHis.size(); //update this
		}
		//chatHisSize = chatHis.size(); //Hopefully the time between the for check and this assignment is very low
		
		//Look through the buffer for valid typing messages
		for (Element chatMsg : elemChat.values()) {
			if (!chatMsg.getAttribute("type").equals("typing") || chatMsg.getTextContent()=="")
				continue; //ignore chat msgs and empty messages
			synchronized (session) {
				session.getBasicRemote().sendText(convertXMLtoString(chatMsg));
			}
		}
		
		//System.out.println("Chat history sent to user "+userClient.IDString());
		return chatHisSize;
	}
	
	private void sendGroupMembers(int groupID) throws Exception {
		//This sends a list of group members NAMES and colors
		//For UI purposes only
		System.out.println("Sending list of group members for group "+groupID);
		//Cant use an attribute so nodes will do
		String msg = "<message type='lGroupMembers' groupID='"+groupID+"'>";
		//HashSet<Client> group = groupManager.getGroup(groupID);
		Set<Client> group = groupManager.getGroup(groupID);
		
		String memList = ""; //regular group members
		String AMChatList = ""; //Admins that are chatting
		String AMMonitorList = ""; //Admins that are monitoring
		
		for (Client c : group) {
			//filter out admin Monitors
			if (c.isAdmin) {
				if (groupManager.getAMGroupStatus(c, groupID)==GroupManager.AM_MONITOR) {
					AMMonitorList = AMMonitorList.concat("<member senderID='"+c.permID+"' senderColor='"+c.chatColor.toString()+"'>"
						+c.username+"</member>");
					continue;
				}
				
				AMChatList = AMChatList.concat("<member senderID='"+c.permID+"' senderColor='"+c.chatColor.toString()+"'>"
						+c.username+"</member>");
				continue;
			}
				
			
			memList = memList.concat("<member senderID='"+c.permID+"' senderColor='"+c.chatColor.toString()+"'>"
					+c.username+"</member>");
			/*msg = msg.concat("<member senderColor='"+c.chatColor.toString()+"'>"
					+c.username+"</member>");
					*/
		}
		
		msg = msg.concat(AMChatList); //add Admins first
		msg = msg.concat(memList);
		msg = msg.concat("</message>");

		System.out.println("Sending "+msg);
		
		for (Client c : group) {
			c.session.getBasicRemote().sendText(msg);
		}
		
	}
	
	/*public synchronized ArrayList<GroupInfoObject> groupStatistics(Client user, int logFileCounter) throws Exception {
		//only update the groups the user is looking for, to the number of logs sent
		HashMap<Integer, Boolean> groupCheckTable = new HashMap<Integer, Boolean>();
		ArrayList<GroupInfoObject> returnGroup = new ArrayList<GroupInfoObject>();
		Boolean[] serverAMStatus = groupManager.getAMStatus(user);
		
		//Guide for the group references- if its in a HashMap, use 1-> numOfGroups
		//If in an array, use 0-> numOfGroups
		//If in xml or GroupInfoObject, use actual group num = groupOffset+1-> groupOffset+1+numOfGroups
		
		for (int i=0; i<groupManager.getNumOfGroups(); i++) {
			groupCheckTable.put(i+1, serverAMStatus[2*i] || serverAMStatus[2*i+1]); 
		}

		ConcurrentHashMap<Integer, GroupInfoObject> groupStat = groupManager.getGroupStats();
		
		int startLog = Integer.MAX_VALUE;
		for (Integer groupID : groupStat.keySet()) {
			if (!groupCheckTable.get(groupID)) //Skip if not being checked
				continue; 
			int lowCheck = groupStat.get(groupID).fileCounter;
			startLog = (lowCheck<startLog) ? lowCheck : startLog; //take lowest
			returnGroup.add(groupStat.get(groupID));
		}
		
		ArrayList<Element> logs = fileLogger.getLoggedClientMessages();
		
		for (int i=startLog; i<logFileCounter; i++) {
			Element log = logs.get(i);
			if(log.getAttribute("type").equals("alert")) //dont run stats on alerts
				continue;
			
			int groupID = Integer.parseInt(log.getAttribute("groupNumber"))-groupManager.groupOffset;
			if (!groupCheckTable.get(groupID)) 
				continue;
			
			//Pull the groupInfoObject
			GroupInfoObject gio = groupStat.get(groupID);
			if (gio.fileCounter>i) //if already logged, skip
				continue;
			
			String username = log.getAttribute("senderName");
			Integer[] userStat = gio.groupMap.get(username);
			if (userStat == null) { //make a new stat if it doesnt exist
				userStat = new Integer[3];
				userStat[0] = 0;
				userStat[1] = 0;
				userStat[2] = 0;
				gio.groupMap.put(username, userStat);
			}
			
			String msgType = log.getAttribute("type");
			if (msgType.equals("typing")) { //add to typing msg
				userStat[0]++;
				gio.totalStats[0]++;
			} else if (msgType.equals("chat")) {
				userStat[1]++;
				gio.totalStats[1]++;
			}
			
			gio.fileCounter = i+1; //update logCounter
		} //end of log loop
		
		//Run the participation algorithm- in percentage
		for (int i=0; i<returnGroup.size(); i++) {
			GroupInfoObject gio = returnGroup.get(i);
			for (String username : gio.groupMap.keySet()) {
				Integer[] userStat = gio.groupMap.get(username);
				double Typing_Weight = 0.7; //value typing more
				double partPercentage = ((double)userStat[0]/gio.totalStats[0])*Typing_Weight
						+((double)userStat[1]/gio.totalStats[1])*(1-Typing_Weight); 
				userStat[2] = (int) (partPercentage*100); 
			}
		}
		
		return returnGroup;
	}*/
	
	public void sendGroupStatistics(Client userClient, ArrayList<GroupInfoObject> gio) throws Exception {
		//Parse the arrayList into a xml object to send to the AM
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		Element rootMsg = doc.createElement("message");
		rootMsg.setAttribute("senderID", userClient.permID);
		rootMsg.setAttribute("type", "groupStatData");
		
		for(int i=0; i<gio.size(); i++) {
			GroupInfoObject gg = gio.get(i);
			Element gs = doc.createElement("groupStat");
			gs.setAttribute("groupNumber", Integer.toString(gg.groupID+groupManager.groupOffset));
			synchronized (gg) {
				for (String username : gg.groupMap.keySet()) {
					Element gm = doc.createElement("memberStat");
					gm.setAttribute("username", username);
					gm.setAttribute("typed", Integer.toString(gg.groupMap.get(username)[0]));
					gm.setAttribute("chatted", Integer.toString(gg.groupMap.get(username)[1]));
					gm.setAttribute("participation", Integer.toString(gg.groupMap.get(username)[2]));
					gs.appendChild(gm);
				}
				
				Element ts = doc.createElement("totalStat");
				ts.setAttribute("typed", Integer.toString(gg.totalStats[0]));
				ts.setAttribute("chatted", Integer.toString(gg.totalStats[1]));
				gs.appendChild(ts);
				
				rootMsg.appendChild(gs);
			}//end  of synchronized block
			
		}//end of iterate over each group
		
		synchronized (userClient.session) {
			userClient.session.getBasicRemote().sendText(convertXMLtoString(rootMsg));
		}
		
	}
	
	public Element sendExitMessage(Document doc, Client userClient, int bGroupID) throws Exception {
		//int bGroupID = userClient.groupID;
		Element exitMsg = doc.createElement("message");
		exitMsg.setAttribute("type", "alert");
		exitMsg.setAttribute("senderID", "Wooz2");
		exitMsg.setAttribute("groupNumber", Integer.toString(bGroupID));
		exitMsg.setAttribute("senderColor", userClient.chatColor.toString());
		exitMsg.setAttribute("senderName", userClient.username);
		exitMsg.appendChild(doc.createElement("text"));
		exitMsg.getFirstChild().setTextContent(userClient.username+" has left the group.");
		
		//Set<Client> group = groupManager.getGroup(bGroupID);
		broadcastGroup(exitMsg, bGroupID);
		sendGroupMembers(bGroupID); //update group members
		
		return exitMsg;
		//System.out.println("Leaving chat AM: PermID: "+userClient.IDString() +" -> group "+userClient.groupID);
	}
	
	public Element sendEnterMessage(Document doc, Client userClient) throws Exception {
		Element broadMsg = doc.createElement("message");
		broadMsg.setAttribute("type", "alert");
		broadMsg.setAttribute("senderID", "Wooz2"); //Server name
		broadMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
		broadMsg.setAttribute("senderColor", userClient.chatColor.toString());
		broadMsg.setAttribute("senderName", userClient.username);
		broadMsg.appendChild(doc.createElement("text"));
		broadMsg.getFirstChild().setTextContent(userClient.username +" has joined the group.");
		
		broadcastGroup(broadMsg, userClient.groupID);
		sendGroupMembers(userClient.groupID);
		return broadMsg;
	}
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0)
	  {
	    System.out.println("Destroying server");
	    if(userCullTimer!=null)
	    	userCullTimer.cancel();
	    if (groupManager!=null)
	    	groupManager.destroy();
	    if(fileLogger!=null)
	    	fileLogger.destroy(); //save files on server exit
	  }
	  
	@Override
	  public void contextInitialized(ServletContextEvent sce)
	  {
	    System.out.println("Initialing chat server"); //called when server context is made
	    
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

//Removes unused userClients to prevent issues with memory
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
			if (user.session==null || !user.session.isOpen()) { //If userClient is disconnected
				System.out.println("Observing user "+user.IDString()+" for inactivity");
				cullList.add(user);
			}
		} //end of for loop
		try {
			Thread.sleep(userReconnectTime); //Give the user time reconnect
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
