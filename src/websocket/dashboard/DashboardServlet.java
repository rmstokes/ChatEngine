package websocket.dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.Timestamp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import container.Group;
//import container
import container.dashStatsContainer;
import util.MessageType;
import websocket.chat.Client;

@WebServlet  //("/DashboardServlet")
@WebListener
@MultipartConfig
@ServerEndpoint(value = "/{path}/dashXML")
public class DashboardServlet extends HttpServlet implements ServletContextListener{
	private static final long serialVersionUID = 1L;
	
	private static final ArrayList<Client> userList = new ArrayList<Client>();
	public static final ArrayList<Session> connectedSessions = new ArrayList<Session>();
	//private static final ArrayList<Client> userList = new ArrayList<Client>();
	
	//private static final String GUEST_PREFIX = "Guest";
	public static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	private static final DateFormat timeDifferenceFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
	private static final Date serverStartTime = new Date();
	
	private Session session;
	private Client userClient;
	private static boolean sessionOpen = false;
	ScheduledExecutorService service;
	
	private static String savedDash = "";
	
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    
    
    
    
    

  
    public DashboardServlet() {
        super();
    }

	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		//System.out.println("doGet is called");
		
		
		String dashXMLString;
		// get printwriters
		PrintWriter out = res.getWriter();
		
		
		res.setContentType("text/xml;charset=UTF-8");
		
		BufferedReader reader = new BufferedReader(req.getReader());
		StringBuffer xmlBuffer = new StringBuffer();
		
		//int k = 0;
		while ((dashXMLString = reader.readLine()) != null) {
			//System.out.println(k + "line:" + dashXMLString);
			//k++;
			if(dashXMLString.startsWith("<?xml") || dashXMLString.startsWith("<dashboard_data>")) {
					xmlBuffer.append(dashXMLString + "\n");
			
			}
			
		}
		dashXMLString = xmlBuffer.toString();
		out.append(dashXMLString);
		if(dashStatsContainer.getInstance().getPath().length() > 0) {
			File xmlSaver = new File(dashStatsContainer.getInstance().getPath()+ 
					sdf.format(new Timestamp(System.currentTimeMillis())) + "xmlLog.txt");
			System.out.println("path: '" + dashStatsContainer.getInstance().getPath()+ "'");
			PrintWriter xmlLog = new PrintWriter(xmlSaver);
			xmlLog.append(dashXMLString);
			xmlLog.append("\n");
			xmlLog.close();
		} else {
			System.out.println("Failed to save xmlLog: no path in dashStatsContainer");
		}
		
		if (dashXMLString.length() > 0) {
			System.out.println("Received XML file");
			try {
				updateDash(dashXMLString);
				savedDash = dashXMLString;
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				System.out.println("No session to reference: NullPointerException");
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("No XML document received");
			out.append("No XML document received");
		}
		
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
	}
	
	@OnError
	public void onError(Throwable t) throws Throwable {
		System.out.println("Chat Error" + t + ": " + t.toString());
	}
	
	@OnOpen
	public void start(Session sessionLoc, @PathParam("path") String path) throws Exception {
		//System.out.println("Succcesssss");
		
		System.out.println("Dashboard opened Websocket @ /" + path + " by sID-" + sessionLoc.getId());
		sessionOpen = true;
		
		//System.out.println("session: " + session.toString());
		//System.out.println("this.session: " + this.session.toString());
		
		this.session = sessionLoc;
		
		//System.out.println("session: " + session.toString());
		//System.out.println("this.session: " + this.session.toString());
		
		//sendXML(session);
		
		Runnable runnable = new Runnable() {
		      public void run() {
		    	  try {
		    		  updateUserNames();
		    		  updateCompletedQCount();
		    		  updateAMs();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		      }
		    };
		    
		    service = Executors
		                    .newSingleThreadScheduledExecutor();
		    service.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
		
		synchronized (connectedSessions) {
			connectedSessions.add(this.session);
		}
		if(savedDash.length() > 0) {
			updateDash(savedDash);
			
			}
		
		//sendXML();
		
	}
	
	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {
		System.out.println("Closing Websocket @ /" + path + " by SID-" + session.getId());
		
		synchronized (connectedSessions) {
			connectedSessions.remove(this.session);
		}
		sessionOpen = false;
		if(service != null) {
			service.shutdown();
		}
		if (!session.isOpen()) {
			System.out.println("Session was closed- ");
			if (userClient != null) {
				System.out.println("Removing user " + userClient.IDString());
			}
			else {
				System.out.println("User not found");
				return;
			}
		}
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		System.out.println("Disconnecting user: " + userClient.IDString() + " -> group " + userClient.groupID);
	}
	
	@OnMessage
	public void incoming(String message, @PathParam("path") String path) throws Exception {
		
		// parse xml message and send broadcast to group.
				// Handles different types of messages
				
				// parse xml
				//System.out.println("this.session: " + this.session.toString());
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				builder = factory.newDocumentBuilder();
				StringReader sr = new StringReader(message);
				InputSource is = new InputSource(sr);
				Document doc = builder.parse(is); //Invalid XML will crash here

				// get message type
				Element element = doc.getDocumentElement();
				String messageType = element.getAttribute("type");
				
				//System.out.println("Received: "+convertXMLtoString(element));

				String senderID = null; 
				
				//Get the senderID and Client and link to this ChatAnnotation class
				senderID = element.getAttribute("senderID");
				
				//long startTime = System.nanoTime();
				
			//Moved these up for optimization, but it only gets like 1ms so idk
		
		if(messageType.equals(MessageType.UserClientAffirm)) {
			//If a clientID is included, confirm it the userClient and send it
			//Else create a new userClient
		//System.out.println("Time diff "+(System.nanoTime()-startTime));
			//Use senderID to find userClient and link userClient
			if (senderID != "") {
				int userListSize = userList.size(); //set before for is run
				
				for (int i=0; i<userListSize; i++) {
					Client user = userList.get(i);
					if (user.permID.equals(senderID)) {
						
						this.userClient = user;
						System.out.println("CONFIRM permID: "+userClient.permID+" -> "+ userClient.toString());
						sendXMLMessage("permIDConfirm", userClient.permID);
						
						/******************************************************
						 * User had a verified Client, run reconnect script
						 *********************************************************/
						
//						//Reconnect for loginChat- is not Admin but has groupID 
//						if (path.equals("loginChat") && !userClient.isAdmin && userClient.groupID>0) {
//							//place user back into group if removed- otherwise Session change fixes this
//							userClient.session = this.session; //change session early in case
//							Set<Client> group = groupManager.getGroup(userClient.groupID);
//							if (!group.contains(userClient))
//								group.add(userClient); 
//
//							sendChatHistory(userClient, 0, true); //send chatHistory
//							sendXMLMessage("goToChat", userClient.permID); //calls swapPanel on loginChatJS which when sent twice
//							//sendXMLMessage("displayChat", userClient.permID); //sends user to chat page
//							
//							sendReconnectMessage(doc, userClient);
//							
//							System.out.println("User "+userClient.IDString()+" has successfully reconnected to loginChat.");
//						} else if //adminMonitor- is admin and has adminMonitor Table (has logged in but not out)
//							(path.equals("adminMonitor") && userClient.isAdmin &&
//									groupManager.getAMStatus(userClient)!=null) {
//							
//							userClient.session = this.session; //refresh session
//							sendAMStatus(groupManager.getAMStatus(userClient)); //client needs this to prepare the windows
//							
//							int[] AMStatus = groupManager.getAMStatus(userClient);
//							for (int groupNo=0; groupNo<AMStatus.length; groupNo++) {
//								if (AMStatus[groupNo]>GroupManager.AM_NONE){ //add to group
//									groupManager.getGroupByNo(groupNo).add(userClient);
//									userClient.groupID = groupManager.getGroupID(groupNo);
//									sendChatHistory(userClient, 0, true);
//								}
//								if (AMStatus[groupNo]==GroupManager.AM_CHAT)
//									sendReconnectMessage(doc, userClient);
//							}
//							System.out.println("AM User "+userClient.IDString()+" has reconnected to: "+AMStatus);
//						}
						break;
					}
				}//end of for loop
				
			}
			
			//if no userClient was found (and by default senderID is empty)
			if (userClient==null) {
				if (senderID != "") {
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
		} //end of UserClientAffirm
	}
	
	private void updateCompletedQCount() throws IOException {
		
		Set<String> keys = dashStatsContainer.getInstance().getGroupKeys();
		
		String msg = "<message type='CorrectQCountUpdate'>";
		
		for(String key:keys) {
			
			int correctCount = dashStatsContainer.getInstance().getGroupCorrectQs(key);
			
			msg += "<entry id='" + key + "' count='" + correctCount + 
					"'></entry>";
		}
		
		
		msg = msg + "</message>";
		//println(msg);
		for (Session session : connectedSessions) {
			//System.out.println("Sent updated CorrectQCount to dash");
			session.getBasicRemote().sendText(msg);
		}
	}
	private void updateAMs() throws IOException {
		
		Set<String> keys = dashStatsContainer.getInstance().getGroupKeys();
		Set<String> adminMonitors;
		
		String msg = "<message type='updateAMs'>";
		
		for(String key:keys) {
			
			adminMonitors = dashStatsContainer.getInstance().getAMs(key);
			for (String adminMonitor : adminMonitors) {
				msg += "<adminMonitor id='" + key + "' uName='" + adminMonitor + 
						"'></adminMonitor>";
			}
		}
		
		
		msg = msg + "</message>";
		//println(msg);
		for (Session session : connectedSessions) {
			//System.out.println("Sent updated CorrectQCount to dash");
			session.getBasicRemote().sendText(msg);
		}
	}
	
	private void updateUserNames() throws IOException{
		Set<String> groupIDs = dashStatsContainer.getInstance().getGroupKeys();
		
		String msg = "<message type='updateUNames'>";
		
		// here is where we get the usernames and put them in groups xml
		for (String groupID : groupIDs) {
			msg += "<group groupname='" + groupID + "'" + ">";
			
			Set<String> users = dashStatsContainer.getInstance().getSetUNames(groupID);
			// here is where we attach the usernames
			for (String uName : users) {
				msg += "<groupmember name='" + uName + "'></groupmember>";
			}
			
			msg += "</group>";
					
		}
		
		msg = msg + "</message>";
		//println(msg);
		for (Session session : connectedSessions) {
			//System.out.println("Sent updated CorrectQCount to dash");
			session.getBasicRemote().sendText(msg);
		}
		
	}
	
	private void updateDash(String dashXMLString) throws Exception {
		
		//System.out.println(dashXMLString);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		StringReader sr = new StringReader(dashXMLString);
		InputSource is = new InputSource(sr);
		Document doc = builder.parse(is); //Invalid XML will crash here
		
		//doc.getDocumentElement().
		//doc.getDocumentElement().setAttribute("type", "DashUpdate");
		
		Document newDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		
		
		
		
		Element e = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("message");
		//System.out.println(dashXMLString);
		e.setAttribute("type", "DashUpdate");
		e.setAttribute("qCount", Integer.toString(dashStatsContainer.getInstance().getQCount()));

		
		newDoc.adoptNode(e);
		newDoc.appendChild(e);
		
		// Create a duplicate node
	    Node newNode = doc.getDocumentElement().cloneNode(true);
	    // Transfer ownership of the new node into the destination document
	    newDoc.adoptNode(newNode);
	    // Make the new node an actual item in the target document
	    newDoc.getDocumentElement().appendChild(newNode);
		
		//println(convertDocumentToString(newDoc));
		
		// This gets all connected sessions from memory and shares dash update
		for (Session session : connectedSessions) {
			System.out.println("Sent update to dash for session: " + session.getId());
			session.getBasicRemote().sendText(convertDocumentToString(newDoc));
		}
		
		
		
		
	}
	
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
	

	
//	public Element sendReconnectMessage(Document doc, Client userClient) throws Exception {
//		Element reconnectMsg = doc.createElement("message");
//		reconnectMsg.setAttribute("type", "alert");
//		reconnectMsg.setAttribute("senderID", "Wooz2");
//		reconnectMsg.setAttribute("groupNumber", Integer.toString(userClient.groupID));
//		reconnectMsg.setAttribute("senderColor", userClient.chatColor.toString());
//		reconnectMsg.setAttribute("senderName", userClient.username);
//		reconnectMsg.appendChild(doc.createElement("text"));
//		reconnectMsg.getFirstChild().setTextContent(userClient.username+" has reconnected to the group!");
//		
//		broadcastGroup(reconnectMsg, userClient.groupID);
//		sendGroupMembers(userClient.groupID);
//		sendGroupAnswerStatus(userClient.groupID);
//		
//		return reconnectMsg;
//	}
	

	public static String convertDocumentToString(Document doc) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		
		return output;
	}
	
	public static String convertXMLtoString(Element node) throws Exception {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		String str = serializer.writeToString(node);
		return str;
	}
	
	public static void println(String s) {
		System.out.println(s);
	}
	private String getFileName(final Part part) {
	    final String partHeader = part.getHeader("content-disposition");
	    
	    for (String content : part.getHeader("content-disposition").split(";")) {
	        if (content.trim().startsWith("filename")) {
	            return content.substring(
	                    content.indexOf('=') + 1).trim().replace("\"", "");
	        }
	    }
	    return null;
	}
}
