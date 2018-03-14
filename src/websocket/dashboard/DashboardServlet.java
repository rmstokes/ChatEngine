package websocket.dashboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import websocket.chat.Client;


@WebServlet  //("/DashboardServlet")
@WebListener
@ServerEndpoint(value = "/{path}/dashXML")
public class DashboardServlet extends HttpServlet implements ServletContextListener{
	private static final long serialVersionUID = 1L;
	
	public static final ArrayList<Session> connectedSessions = new ArrayList<Session>();
	//private static final ArrayList<Client> userList = new ArrayList<Client>();
	
	private Session session;
	private Client userClient;
	private static boolean sessionOpen = false;
  
    public DashboardServlet() {
        super();
    }

	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("doGet is called");
		
		String responseXMLString;
		PrintWriter out = res.getWriter();
		res.setContentType("text/xml;charset=UTF-8");
		
		BufferedReader reader = new BufferedReader(req.getReader());
		StringBuffer xmlBuffer = new StringBuffer();
		
		while ((responseXMLString = reader.readLine()) != null) {
			xmlBuffer.append(responseXMLString + "\n");
		}
		responseXMLString = xmlBuffer.toString();
		out.append(responseXMLString);
		
		if (responseXMLString.length() > 0) {
			System.out.println("Recieved XML file");
			try {
				sendXML();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				System.out.println("No session to reference");
				e.printStackTrace();
			}
		}
		else {
			System.out.println("No XML document recieved");
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
	@OnOpen
	public void start(Session session, @PathParam("path") String path) throws Exception {
		//System.out.println("Succcesssss");
		
		System.out.println("Opened Websocket @ /" + path + " by sID-" + session.getId());
		sessionOpen = true;
		this.session = session;
		
		//sendXML(session);
		
		synchronized (connectedSessions) {
			connectedSessions.add(this.session);
		}
		
	}
	
	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {
		System.out.println("Closing Websocket @ /" + path + " by SID-" + session.getId());
		
		synchronized (connectedSessions) {
			connectedSessions.remove(this.session);
		}
		sessionOpen = false;
		
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
	public void incoming(String message, @PathParam("path") String path) throws Exception {}
	
	private void sendXML() throws IOException {
		
//		Element e = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("message");
//		
//		e.setAttribute("type", "groupInfo");
//		System.out.println(convertXMLtoString(e));
		
		String msg = "This is a test message";
		//System.out.println(this.session.toString());
		
		session.getBasicRemote().sendText(msg);
		
	}
	
private void sendXML(Session session) throws IOException {
		
//		Element e = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("message");
//		
//		e.setAttribute("type", "groupInfo");
//		System.out.println(convertXMLtoString(e));
		
		String msg = "This is a test message";
		System.out.println(session.toString());
		
		session.getBasicRemote().sendText(msg);
		
	}
	
	public static String convertXMLtoString(Element node) throws Exception {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		String str = serializer.writeToString(node);
		return str;
	}

}
