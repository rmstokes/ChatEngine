package HttpReqHandler;



import java.io.BufferedReader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@WebServlet
public class HttpReqHandlerClass extends HttpServlet {
	/**
	 *  this is cheating, needs to be fixed so it is getting the log param from web.xml
	 */
	private String logPath = "/home/kimlab/newserver_test/logs/"; 
																	
	private String sFileName = "currentLogFileNames.txt";
	

	/*@Override
	public void init(ServletConfig config) throws ServletException {
		this.logPath = config.getInitParameter("logPath");
	}*/
	
	public HttpReqHandlerClass() throws UnsupportedEncodingException {
		/*String  logPathMaybe = this.getInitParameter("logPath");
		System.out.println(logPathMaybe);
		this.logPath = logPathMaybe;*/
		
		
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException{
		
		String typeParam = req.getParameter("type");
		String responseXMLString;
		switch(typeParam) {
		case "logRequest": 
			responseXMLString = getLogsAsXML();
			
			break;
		}
		
	}
	/*
	 * This function returns a string that contains all conversation logs 
	 * up to this point
	 * 
	 * currently having issues with actually parsing the file. parsing a null value.
	 * No arguments
	 */
	private String getLogsAsXML() throws IOException{
		
		File iFile = new File(logPath+sFileName);
		/**
		 * Will need to 
		 */
		if (!iFile.exists()) {
			return "<Error><text>There is no session open</text></Error>";
		}
		
		BufferedReader br = new BufferedReader(new FileReader(iFile));
		try {
			StringBuilder sb = new StringBuilder();
			String logName = "";
			while((logName = br.readLine()) != null) { 
				try {
					System.out.println(logName);
					File xmlFile = new File(logName);
					System.out.println("read in:" + xmlFile);
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(xmlFile);
					System.out.println(doc);
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
		} finally {
			br.close();
		}
		
		
		
		
		return null;
	}
	
	

}
