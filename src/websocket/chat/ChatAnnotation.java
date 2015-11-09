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
import java.util.AbstractSet;
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
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import util.HTMLFilter;

@ServerEndpoint(value = "/{path}")
public class ChatAnnotation {

	private static final Log log = LogFactory.getLog(ChatAnnotation.class);
	
	private static final String GUEST_PREFIX = "Guest";
	private static final AtomicInteger connectionIds = new AtomicInteger(0);
	private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<>();
		
	private static String nickname;
	private Session session;
	//private static String adminID = null;
	private static boolean adminCreatedGroups = false;
	private static Hashtable<Integer, HashSet<String>> groupTable = new Hashtable<Integer, HashSet<String>>();

	public ChatAnnotation() {
		//a new ChatAnnotation object is created for every Client page that connects to server.
		System.out.println("chat annotation constructor");
	}

	@OnOpen
	public void start(Session session, @PathParam("path") String path) {
		//onopen called when client side websocket initiates connection to this server.
		//path argument identifies which client page is on other end
		//for 'landing' page connections add the ChatAnnotation object to static list.
		System.out.println("inside start OnOpen");
		System.out.println("path: " + path);
		this.session = session;
		System.out.println("real session id: " + session.getId());
		
		switch (path) {
		case "admin":
			System.out.println("admin connected.");			
			return;
		case "login":
			// if admin has not created groups then don't allow client to
			// connect.
			System.out.println("login page connected.");
			return;		
		case "chat":
			// if admin has not created groups then don't allow client to
			// connect.
			System.out.println("chat page connected.");			
			System.out.println("adminCreatedGroups: " + adminCreatedGroups);
			return;
		case "landing":
			System.out.println("landing page connected.");
			// save connections originating from landing page.
			connections.add(this);
			return;
		default:
			return;

		}
		
	}

	@OnClose
	public void end() {
		connections.remove(this);
		// remove user from appropriate group

		// need to change this to xml format
		String message = String.format("* %s %s", nickname, "has disconnected...");

		try {
			broadcast(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		Document doc = builder.parse(is);

		// get message type 
		Element element = doc.getDocumentElement();
		String messageType = element.getAttribute("type");
		
		String senderID = null;
		if (messageType.equals("typing") || messageType.equals("chat")) {
			// add origin senderID attribute to the document before broadcasting
			// it.						
			senderID = element.getAttribute("senderID");			
			// now that the senderID has been added to document
			// transform the updated document into a string before broadcasting
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			message = writer.toString();
			System.out.println("broadcasting message");
			broadcast(message);
		} else if (messageType.equals("groupCreation")) {
			// this is message from admin page to create groups
			int numGroups = Integer.parseInt(element.getTextContent());
			System.out.println("numGroups: " + numGroups);
			// create the groups
			// can check here if admin is still connected to prevent multiple
			// admins

			for (int key = 1; key <= numGroups; key++) {
				// create empty linkedlists for every key.
				HashSet<String> value = new HashSet<String>();
				groupTable.put(key, value);
			}
			adminCreatedGroups = true;
			System.out.println("groups created");

			// send message to client to invoke a new web page that displays
			// group creation confirmation and link to login page possibly
			senderID = element.getAttribute("senderID");
			String msg = "<message type='groupsCreated' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg);
			
			
		} else if (messageType.equals("setUserType")) {
			// this is a message from landing page drop down menu
			// session contains the initial client id.

			/***** this is where each client's id is assigned. ****/

			senderID = session.getId();
			System.out.println("message: " + message);
			System.out.println("element: " + element.getTextContent());
			System.out.println("senderID: " + senderID);
			String userType = element.getTextContent();

			switch (userType) {
			case "Login":
				// return student login page
				String msg1 = "<message type='displayLogin' senderID='" + senderID + "'></message>";
				session.getBasicRemote().sendText(msg1);
				break;
			case "Create Groups":
				// return admin page
				String msg2 = "<message type='displayAdmin' senderID='" + senderID + "'></message>";
				session.getBasicRemote().sendText(msg2);
				// System.out.println("sent html to client.");
				break;
			default:
				System.out.println("Error: check userType");
				return;
			}
		} else if (messageType.equals("joinGroup")) {
			// add user to proper group
			String nickname = element.getAttribute("username");
			// nickname = username;
			senderID = element.getAttribute("senderID");
			System.out.println("joinGroup message received.");
			System.out.println("senderID: " + senderID);
			String groupNumber = element.getTextContent();
			System.out.println("joining group: " + groupNumber);
			HashSet<String> group = groupTable.get(Integer.parseInt(groupNumber));
			System.out.println("retrieved LL");
			boolean addedToList = group.add(senderID);

			// send message to move client browser to chat page
			String msg3 = "<message type='displayChat' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg3);
			System.out.println("addedToList: " + addedToList);
			

		} else if (messageType.equals("requestID")) {
			senderID = session.getId();
			String msg4 = "<message type='alert' senderID='" + senderID + "'></message>";
			session.getBasicRemote().sendText(msg4);
		} else if (messageType.equals("broadcast")) {
			// map the current session's sender id to the connection id
			// established on initial
			// connection from landing page

			// send message to group indicating who has just joined.
			System.out.println("broadcast message type detected.");
			String nickname = element.getAttribute("username");
			senderID = element.getAttribute("senderID"); // original senderID
			System.out.println("senderID: "+senderID);
			// String msg5 = "<message type='alert'
			// senderID='"+senderID+"'><text>"+nickname+" has joined
			// group.</text></message>";

			// establish link between current session.id() and original senderID
			// these values will be different so they need to be mapped to one
			// another
			// so that we can respond to the proper users within a group
			
			// place new senderID into group and remove old senderID from group
			Integer originalGroupNumber = getSenderGroup(senderID);
			HashSet<String> group = groupTable.get(originalGroupNumber);
			group.remove(senderID);
			group.add(session.getId());

			// send message to group that new user has joined session.
			String updatedID = session.getId();
			String msg5 = "<message type='alert' senderID='" + senderID + "' updatedID='" + updatedID
					+ "' groupNumber='" + originalGroupNumber + "'><text>"+nickname+" has joined group.</text></message>";
			System.out.println("calling broadcast with alert message.");
			broadcast(msg5);
		}
	}

	@OnError
	public void onError(Throwable t) throws Throwable {
		log.error("Chat Error: " + t.toString(), t);
	}

	private void broadcast(String msg) throws Exception {
		// broadcast message to members of same group.

		// access xml Document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		StringReader sr = new StringReader(msg);
		InputSource is = new InputSource(sr);
		Document doc = builder.parse(is);

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
			//set senderGroup based on xml attribute
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
		System.out.println("updatedID: "+element.getAttribute("updatedID"));
		// set variable to store clientIDs for senderGroup
		HashSet<String> broadcastGroupMembers = groupTable.get(senderGroup);

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
	}

	private Integer getSenderGroup(String senderID) {
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
					break;
				}
			}
		}
		return senderGroup;
	}
}
