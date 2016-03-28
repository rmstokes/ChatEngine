package websocket.chat;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*This is the File logging class for the Chat server.
 * The class will be reset if the groups are reset
 * 
* We can let Tomcat save the Errors that go through the tomcat.juli.Logger class
* 
* If we need the server log information (like what senderID is assigned, certain routines
* like XML and etc) we can wrap the System.out.println function or just replace the
* System.out.println with Logger class.
*/

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import websocket.chat.ChatAnnotation;

public class FileLogger extends TimerTask {
	
	private static long FILE_UPDATE_TIME = 1000*60; //Run every minute
	
	//Variables that holds ALL client messages - Not a buffer
	//Find the best data structure for this, 1 large xml file or an array of xml elements?
	//For getting group chat history, we should filter this list for groupNo & chat type
	private ArrayList<Element> loggedClientMessages = new ArrayList<Element>();
	
	//Counter for number of elements that have been saved to file (since this is not a buffer)
	private long fileCounter = 0;
	private static final Log log = LogFactory.getLog(FileLogger.class);
	private static Timer fileTimer;
	
	private final int groupNum;
	private final int groupIteration;
	
	private static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	private final Date startDate;
	
	private final String instructor;
	private boolean destroy = false;
	private String logPath;
	
	public FileLogger(int groupNum, Date date, int groupIt, String logPath, String instruct) {
		System.out.println("FileLogger constructor "+new Date().toString());
		fileTimer = new Timer();
		fileTimer.schedule(this, FILE_UPDATE_TIME, FILE_UPDATE_TIME);
		
		this.groupNum = groupNum;
	    this.startDate = date;
	    this.groupIteration = groupIt;
	    this.instructor = instruct;
	    this.logPath = logPath;
	}
	
	//Incomplete-
	public boolean captureMessage(Element e) {
		try {
			//add server timestamp to this xml element
			loggedClientMessages.add(e);
			System.out.println("Captured client message");
			return true;
		} catch (Error err) {
			log.error(err);
			return false;
		}
	}
	
	//for retrieving chat history- should not be structurally changed
	public ArrayList<Element> getLoggedClientMessages() {
		//Element[] e = {};
		//if (loggedClientMessages==null) return e; //return empty array
		return loggedClientMessages;
		
	}
	
	public void destroy () {
		destroy = true;
		run(); //save xml for the last time
		fileTimer.cancel(); //remove timer
	}
	
	public void run () {
		//Here check the fileCounter and save new messages to file
		
		System.out.println("File Save Date: "+new Date().toString());
		
		try {
			saveXML();
		} catch (Exception e) {
			System.out.println("Problem saving XML");
		}

	}
	
	public void saveXML() throws Exception {
		
		Date endDate = new Date(); //Date for time of saving xml
		
		if (loggedClientMessages.size()==fileCounter) {
			System.out.println("No new messages have been logged - "+serverDateFormatter.format(endDate));
			return;
		}
		
		fileCounter = loggedClientMessages.size();
	    
	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	    
	    //create documents & root element for each group
	    Document[] docs = new Document[this.groupNum];
	    Element[] root = new Element[this.groupNum];
	    
	    //loop and generate new Document + new Root element for xml
	    for (int i = 0; i < this.groupNum; i++)    {
	      docs[i] = documentBuilder.newDocument();
	      
	      root[i] = docs[i].createElement("woozlog");
	      
	      //Add data for each file
	      root[i].setAttribute("GroupIteration", Integer.toString(this.groupIteration));
	      root[i].setAttribute("GroupName", "Group " + i);
	      root[i].setAttribute("Instructor", this.instructor);
	      root[i].setAttribute("StartTime", serverDateFormatter.format(this.startDate));
	      if (this.destroy) {
	        root[i].setAttribute("EndTime", serverDateFormatter.format(endDate));
	      } else {
	        root[i].setAttribute("LastUpdateTime", serverDateFormatter.format(endDate));
	      }
	      docs[i].appendChild(root[i]);
	    }
	    
	    //Loop through each message, add to corresponding document
	    for (Element e : this.loggedClientMessages) {
	      int groupID = Integer.parseInt(e.getAttribute("groupNumber")) - 1;
	      Node ne = docs[groupID].importNode(e, true);
	      root[groupID].appendChild(ne);
	    }
	    
	    
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    
	    System.out.println("Saving to path " + this.logPath);
	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	    
	    //save xml file for each group
	    for (int i = 0; i < this.groupNum; i++)  {
	      DOMSource source = new DOMSource(docs[i]);
	      String filename = "Chatlog_Iter_" + this.groupIteration + "_Group" + (i + 1) + " " + df.format(this.startDate) + ".xml";
	      
	      this.logPath = this.logPath.replaceAll("\\\\+", "\\\\"); //gotta DOUBLE DELIMIT on windows
	      filename = filename.replaceAll("\\/", "-"); //get rid of / in date
	      filename = filename.replaceAll(":", "-"); //get rid of : in time
	      filename = filename.replaceAll(" ", "_"); //replace space with _
	      
	      new File(this.logPath).mkdir(); //create new file directory if DNE
	      File logFile = new File(this.logPath + filename);
	      logFile.setReadable(true); //make the logFile readable from linux
	      StreamResult streamresult = new StreamResult(logFile);
	      
	      transformer.transform(source, streamresult);
	    }
	}

}
