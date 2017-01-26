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
	
	private final GroupManager gManage;
	//private final int groupNum;
	private final int groupIteration;
	
	private static final DateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	public Date endDate;
	
	//private final String instructor;
	private boolean destroy = false;
	private String logPath;
	
	public FileLogger(GroupManager gm, int groupIt, String logPath) {
		//int groupNum, Date date, int groupIt, String logPath, String instruct
		System.out.println("FileLogger constructor @ "+logPath+" Set:"+groupIt);
		fileTimer = new Timer();
		fileTimer.schedule(this, FILE_UPDATE_TIME, FILE_UPDATE_TIME);
		
		this.gManage = gm;
		//this.groupNum = gm.groupTotal;
	    //this.startDate = date;
	    this.groupIteration = groupIt;
	    //this.instructor = instruct;
	    this.logPath = logPath;
	}
	
	
	public boolean captureMessage(Element e) {
		try {
			//add server timestamp to this xml element
			loggedClientMessages.add(e);
			System.out.print("Captured broadcast ->");
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
		fileTimer.cancel(); //remove timer
		run(); //save xml for the last time
		System.out.println("Final logs saved, FileLogger Shutdown");
	}
	
	public void run () {
		//Here check the fileCounter and save new messages to file
		
		System.out.println("File Save Start: "+new Date().toString());
		
		try {
			saveXML();
		} catch (Exception e) {
			System.out.println("Problem saving XML "+e.getMessage()+" "+e.toString());
			fileCounter = 0; //if file not saved- try again
		}

	}
	
	private int maxSleep = 30; //mins
	public void saveXML() throws Exception {

	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); //File format date string
		
	    //So it sleeps for 1-5mins
	    //To close the Set, it must run for 15mins without ANY messages sent
	    int sleepTime = 1;
		int actualTime = 0;
		
		while (loggedClientMessages.size()==fileCounter) {
			actualTime += sleepTime;
			
			if (destroy) 
				break; //ignore this sleep timer
			System.out.println("No new messages have been logged for "+actualTime+"mins as of - "+df.format(new Date()));
			
			//sleepTIme > maxSleep 
			if (actualTime>=maxSleep) {
				System.out.println("No messages have been logged for "+actualTime+"mins. Server will close the Set and save files.");
				ChatAnnotation.groupManager.destroy();
				ChatAnnotation.groupManager = null; //destroy reference
				ChatAnnotation.fileLogger.destroy();
				ChatAnnotation.fileLogger = null;
				return;
			}
			Thread.sleep(1000*60*sleepTime++); //sleep
			//sleepTime++;
		}
		
		
		fileCounter = loggedClientMessages.size();
		
		endDate = new Date(); //Date for time of saving xml
	    
	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	    
	    //create documents & root element for each group
	    Document[] docs = new Document[gManage.groupTotal];
	    Element[] root = new Element[gManage.groupTotal];
	    
	    //loop and generate new Document + new Root element for xml
	    for (int i = 0; i < gManage.groupTotal; i++)    {
	      docs[i] = documentBuilder.newDocument();
	      
	      root[i] = docs[i].createElement("woozlog");
	      
	      //Add data for each file
	      //root[i].setAttribute("GroupIteration", Integer.toString(this.groupIteration));
	      root[i].setAttribute("GroupIteration", Integer.toString(this.groupIteration));
	      root[i].setAttribute("GroupName", "Group " + gManage.getGroupID(i));
	      root[i].setAttribute("Instructor", this.gManage.logName);
	      root[i].setAttribute("StartTime", serverDateFormatter.format(this.gManage.setCreateDate));
	      if (this.destroy) {
	        root[i].setAttribute("EndTime", serverDateFormatter.format(endDate));
	      } else {
	        root[i].setAttribute("LastUpdateTime", serverDateFormatter.format(endDate));
	      }
	      docs[i].appendChild(root[i]);
	    }
	    
	    //Loop through each message, add to corresponding document
	    //for (Element e : this.loggedClientMessages) {
	    int logSize = loggedClientMessages.size();
	    
	    for (int i=0; i<logSize; i++) {
	      
	      Element e = loggedClientMessages.get(i);
	      
	      int groupID = Integer.parseInt(e.getAttribute("groupNumber")) - 1;
	      
	      int indexID = groupID - gManage.groupOffset;
	      
	      if(indexID<0 || indexID>gManage.groupTotal) return;
	      
	      Node ne = docs[indexID].importNode(e, true);
	      
	      root[indexID].appendChild(ne);
	    }
	    
	    
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	    
	    System.out.println("Saving to path " + this.logPath+" @ "+df.format(endDate));
	    
	    //save xml file for each group
	    for (int i = 0; i < gManage.groupTotal; i++)  {
	      DOMSource source = new DOMSource(docs[i]);
	      String filename = "Chatlog_Group" + gManage.getGroupID(i) + "_" + df.format(gManage.setCreateDate) + ".xml";
	      
	      this.logPath = this.logPath.replaceAll("\\\\+", "\\\\"); //gotta DOUBLE DELIMIT on windows
	      filename = filename.replaceAll("\\/", "-"); //get rid of / in date
	      filename = filename.replaceAll(":", "-"); //get rid of : in time
	      filename = filename.replaceAll(" ", "_"); //replace space with _
	      
	      String fileSep = System.getProperty("file.separator");
	      String logPathDir = this.logPath + "SetIter_"+groupIteration+"_"+df.format(gManage.setCreateDate)+"_"+gManage.logName+fileSep;
	      
	      logPathDir = logPathDir.replaceAll(" ", "_"); //space can sneak in with instructor
	      File logPathFile = new File(logPathDir); 
	      logPathFile.mkdir(); //create new file directory if DNE 
	    
	      logPathFile.setExecutable(true, false); //This gives linux dir read/execute perm so kimlab can read root dirs
	      logPathFile.setReadable(true, false); //So kimlab can view the dir & contents, but cannot edit it
	      //Final permissions should be 755 rwxr-xr-x
	      
	      File logFile = new File(logPathDir + filename);
	      logFile.setReadable(true, false); //make the logFile readable from linux
	      //Final permissions should be 644 rw-r--r--
	      StreamResult streamresult = new StreamResult(logFile);
	      
	      transformer.transform(source, streamresult);
	      
	    }
	}

}
