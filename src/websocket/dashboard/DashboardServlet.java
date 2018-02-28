package websocket.dashboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;


@WebServlet  //("/DashboardServlet")
public class DashboardServlet extends HttpServlet implements ServletContextListener{
	private static final long serialVersionUID = 1L;
       
  
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
		
	}
	
	@OnClose
	public void end(Session session, @PathParam("path") String path) throws Exception {}
	
	@OnMessage
	public void incoming(String message, @PathParam("path") String path) throws Exception {}

}
