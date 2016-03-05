package websocket.chat;

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

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Element;

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
	
	public FileLogger() {
		System.out.println("FileLogger constructor "+new Date().toString());
		fileTimer = new Timer();
		fileTimer.schedule(this, FILE_UPDATE_TIME, FILE_UPDATE_TIME); 
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
		return loggedClientMessages;
	}
	
	
	public void run () {
		//Here check the fileCounter and save new messages to file
		System.out.println("Saving to file! "+new Date().toString());
		
	}

}
