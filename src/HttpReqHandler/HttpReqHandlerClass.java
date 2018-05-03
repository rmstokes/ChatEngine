package HttpReqHandler;



import java.io.BufferedReader;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

// import container
import container.*;

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
		PrintWriter out = res.getWriter();
		switch(typeParam) {
		case "logRequest": 
			res.setContentType("text/xml;charset=UTF-8");
			responseXMLString = getLogsAsXML();
			out.append(responseXMLString);
			break;
		default: System.out.println("There was an incorrect request sent to HttpReqHandler");
				
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
		BufferedReader br = null;
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder1 = null;
		DocumentBuilder dBuilder2 = null;
		/**
		 * Will need to 
		 */
		
		
		
		try {
			dBuilder1 = dbFactory.newDocumentBuilder();
			dBuilder2 = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Document newDoc = dBuilder1.newDocument();
		try {
			br = new BufferedReader(new FileReader(iFile));
		} catch (Exception e2) {
			// iFile doesn't exist
			System.out.println("Attempted to read logNames and failed\n" +
			"No file found at: " + iFile);
			
			return generateErrorXML(newDoc);
		}
		/*if (!iFile.exists()) {
			br.close();
			
			
			return generateErrorXML(newDoc);
		}*/
		try {
			
			Element element = newDoc.createElement("woozlogs");
			newDoc.appendChild(element);
			String logName = "";
			while((logName = br.readLine()) != null) { 
				try {
					//System.out.println(logName);
					File fXmlFile = new File(logName);
					
					Document doc = dBuilder2.parse(fXmlFile);
					
					doc.getDocumentElement().normalize();
					//System.out.println(toString(doc));
					
					//Node transfer = doc.getDocumentElement().getFirstChild();
					//woozlogs.appendChild(doc.getElementById("woozlog"));
					NodeList nodesToCopy = doc.getElementsByTagName("woozlog");
					//System.out.println("Nodes: " + nodesToCopy.getLength());
					
					for(int i =0; i < nodesToCopy.getLength(); i++) {
					    /// Create a duplicate node
					    Node newNode = nodesToCopy.item(i).cloneNode(true);
					    String name = newNode.getLocalName();
					    
					    // Transfer ownership of the new node into the destination document
					    newDoc.adoptNode(newNode);
					    // Make the new node an actual item in the target document
					    newDoc.getDocumentElement().appendChild(newNode);
					}
					
					

					
					
				}  catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			//woozlogs.appendChild(buildElement);
			
			
		} finally {
			br.close();
		}
		
		
		
		
		return toString(newDoc);
	}
	


private static String toString(Document doc) {
    try {
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    } catch (Exception ex) {
        throw new RuntimeException("Error converting to String", ex);
    }
}

private static String generateErrorXML(Document newDoc) {
	newDoc.appendChild(newDoc.createElement("Error"));
	newDoc.getDocumentElement().appendChild(newDoc.createTextNode("There is no session open"));
	
	return toString(newDoc);
}


	
	

}
