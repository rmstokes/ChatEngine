package websocket.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.Session;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import container.dashStatsContainer;
import util.CHAT_COLOR;
import util.GroupInfoObject;

public class GroupManager {
	/*
	 * Wrapper for the groups- manages the group info, chat history and colours; etc
	 */
	private static final Log log = LogFactory.getLog(FileLogger.class);
	
	public final String logName;
	public final Date setCreateDate;
	public final int groupOffset;
	public final int groupTotal;
	public static CHAT_COLOR TA_COLOR = CHAT_COLOR.Red;
	public static CHAT_COLOR TA2_COLOR = CHAT_COLOR.Tomato;
	public static CHAT_COLOR TA3_COLOR = CHAT_COLOR.Crimson;
	public static CHAT_COLOR SERVER_COLOR = CHAT_COLOR.Black;
	
	public static final int AM_MONITOR = 2;
	public static final int AM_CHAT = 1;
	public static final int AM_NONE = 0;
	
	//List of Clients for each group
	//ConcurrentHashMap instead of HashTable cause the latter is deprecated
	//CopyOnWriteArraySet since HashSet is less efficient for high number of reads
	//CopyOnWrite also means that broadcasts dont need to be synchronized
	private ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> > groupTable = new ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> >();
	
	//Answer strings are stored here
	public String[] answer;
	public String[] prevAnswer;
	public boolean[] answerLock;
	public long[] answerTypeTime;
	public String[] answerTypeID;
	
	//List of AdminMonitors and their information
	private ConcurrentHashMap<Client, int[]> adminMonitorTable = new ConcurrentHashMap<Client, int[]>();
	
	private ConcurrentHashMap<Integer, GroupInfoObject> groupStatistics = new ConcurrentHashMap<Integer, GroupInfoObject>();
	
	public GroupManager(int numGroups, int groupOffset, String logName) {
		System.out.println("Group Manager Constructor: Group Total:"+numGroups + " Group Offset:"+groupOffset +" Log Name: "+logName); 
		
		//Populate group list
		for (int key = 1; key <= numGroups; key++) {
			//HashSet<Client> value = new HashSet<Client>();
			CopyOnWriteArraySet<Client> value = new CopyOnWriteArraySet<Client>();
			groupTable.put(key, value);
			groupStatistics.put(key, new GroupInfoObject(key));
			System.out.println("in GroupManager groupOffset: "+ groupOffset + " key: " + key);
			// input group numbers to dashStatsContainer
			dashStatsContainer.getInstance().initializeGroup(+ groupOffset + key); 
		}
		
		this.logName = logName;
		this.groupOffset = groupOffset;
		this.groupTotal = numGroups;
		
		setCreateDate = new Date();
		
		answer = new String[groupTotal];
		prevAnswer = new String[groupTotal];
		answerLock = new boolean[groupTotal];
		answerTypeTime = new long[groupTotal]; 
		answerTypeID = new String[groupTotal];
		
		for (int i=0; i<groupTotal; i++) {
			answer[i] = "";
			prevAnswer[i] = "System: No answer";
			//answerLock[i] = false;
			answerTypeTime[i] = 0;
			answerTypeID[i] = "";
		}
	}
	
	public boolean joinGroup(int groupID, Client user) {
		Set<Client> group = getGroup(groupID);
		try {
			return group.add(user);
		} catch (Error e) {
			log.error(e);
			return false;
		}
		
	}
	
	public boolean assignChatColor(Client user) {
		//Choose color value at random
		CHAT_COLOR[] chatColorArr = CHAT_COLOR.values();
		boolean[] recordArr = new boolean[chatColorArr.length];
		Set<Client> group = getGroup(user.groupID);
		
		for (Client groupMem : group) { //get currently used color values
			if (groupMem==user) continue;
			recordArr[groupMem.chatColor.ordinal()] = true;
		}
		
		//if (user.username.startsWith("TA") ) {
		if (user.isAdmin) {
			user.chatColor = TA_COLOR;
			if (!recordArr[TA_COLOR.ordinal()])
				return true;//do nothing
			else if (recordArr[TA_COLOR.ordinal()]&&!recordArr[TA2_COLOR.ordinal()])
				user.chatColor = TA2_COLOR;
			else if (recordArr[TA2_COLOR.ordinal()])
				user.chatColor = TA3_COLOR;
			
			return true;
		}
		
		//exclude TA color & Server color
		recordArr[TA_COLOR.ordinal()] = true;
		recordArr[TA2_COLOR.ordinal()] = true;
		recordArr[TA3_COLOR.ordinal()] = true;
		recordArr[SERVER_COLOR.ordinal()] = true;
		
		for (int i=0; i<chatColorArr.length; i++) {
			int rand = (int)(Math.random()*chatColorArr.length);
			if (!recordArr[rand]) {
				user.chatColor = chatColorArr[rand];
				break;
			} else if (!recordArr[i]) {
				user.chatColor = chatColorArr[i];
				break;
			}
		}
		if (user.chatColor==null) user.chatColor = CHAT_COLOR.Black;
		return user.chatColor!=null;
	}
	
	//This is normalized for groupOffset
	public Set<Client> getGroup(int groupID) {
		return groupTable.get(groupID-groupOffset);
	}
	
	public Set<Client> getGroupByNo(int groupNo) {
		return groupTable.get(groupNo+1);
	}
	
	public int getGroupID(int groupNo) {
		return groupNo + groupOffset+1;
	}
	
	public int getGroupNo(int groupID) {
		return groupID - groupOffset -1;
	}
	
	//destroy
	public boolean destroy() {
		
		try { //broadcast message for logs & current users
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element closeGroupMsg = doc.createElement("message");
			closeGroupMsg.setAttribute("type", "alert");
			closeGroupMsg.setAttribute("senderID", "Wooz2");
			closeGroupMsg.setAttribute("timestamp", ChatAnnotation.serverDateFormatter.format(new Date()));
			closeGroupMsg.appendChild(doc.createElement("text"));
			closeGroupMsg.getFirstChild().setTextContent("This set has been closed.");
			
			for (int i=0; i<groupTotal; i++) {
				Element groupCloseMsg = (Element) closeGroupMsg.cloneNode(true);
				groupCloseMsg.setAttribute("groupNumber", Integer.toString(getGroupID(i)));
				ChatAnnotation.broadcastGroup(groupCloseMsg, getGroupID(i)); //broadcast to groups
				
				Set<Client> group = groupTable.get(i+1);
				for (Client uc : group) {
					uc.groupID = -1; //-1 means not in a group
					//Dont need to remove since groups are dropped
				}
			}
		
		} catch (Exception pce) {
			System.out.println("Failed to create closing Group message. (GroupManager)");
		}
		
		try { //send set close message to all clients
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element setCloseMsg = doc.createElement("message");
			setCloseMsg.setAttribute("type", "setClose");
			setCloseMsg.setAttribute("timestamp", ChatAnnotation.serverDateFormatter.format(new Date()));
			
			String setCloseStr = ChatAnnotation.convertXMLtoString(setCloseMsg);
			
			for (int i=0; i<ChatAnnotation.connectedSessions.size(); i++) {
				ChatAnnotation.connectedSessions.get(i).getBasicRemote().sendText(setCloseStr);
			}
			
		} catch (Exception e) {
			System.out.println("Failed to create closing Set message. (GroupManager)");
		}
		
		
		//AdminMonitors should already be dropped from each group
		//Remove adminStatus
		for (Client userClient : adminMonitorTable.keySet())
			userClient.isAdmin = false;
		
		//adminMonitorTable gets dropped so adminMonitors lose their registry
		/*for (int i=0; i<adminMonitorTable.size(); i++) {
			adminMonitorTable.remove(adminMonitorTable.get(0)); //remove first index til none exist
		}*/
		//They will lose adminStatus automatically since they have no data stored on userClient
		//And will receive updated group info
		return true;
	}
	
	//add new Admin Monitor
	public boolean addAdminMonitor(Client userClient) {
		int[] monitorTable = new int[groupTotal]; //2 slots per group
		for (int i=0; i<monitorTable.length; i++)
			monitorTable[i] = 0;
		
		try {
			adminMonitorTable.put(userClient, monitorTable);
			userClient.isAdmin = true;
			
			return adminMonitorTable.containsKey(userClient);
		} catch (Error e) {
			log.error(e);
			return false;
		}
	}
	
	public boolean dropAdminMonitor (Client userClient) {
		int[] val = adminMonitorTable.remove(userClient);
		return val!=null;
	}
	
	//int 0=none, 1=chat, 2=monitor
	public int[] getAMStatus(Client userClient) {
		return adminMonitorTable.get(userClient);
	}
	
	public int[] setAMStatus(Client userClient, int[] AMStatus) {
		return adminMonitorTable.put(userClient, AMStatus);
	}
	
	public int getAMGroupStatus (Client userClient, int groupID) {
		return adminMonitorTable.get(userClient)[groupID-this.groupOffset-1];
	}
	
	public boolean getAnswerLock (int groupID) {
		return answerLock[getGroupNo(groupID)];
	}
	
	//Used to block new users typing in between the 3s interval
	long answerTimeout= 3*1000; //3 seconds
	
	public boolean allowAnswerType (int groupID, String senderID) {
		int groupNo = getGroupNo(groupID);
		long timeDiff = System.currentTimeMillis() - answerTypeTime[groupNo];
		
		
		boolean answerTypePriority = senderID.equals(answerTypeID[groupNo]) || timeDiff>answerTimeout;
		
		if (answerTypePriority) {//new typer takes priority if true
			answerTypeID[groupNo] = senderID;
			//update the time
			answerTypeTime[groupNo] = System.currentTimeMillis();
		}
		
		return answerTypePriority;
	}
	
	public void setAnswerLock (int groupID, boolean value) {
		answerLock[getGroupNo(groupID)] = value;
	}
	
	public void setAnswer (int groupID, String ans) {
		answer[getGroupNo(groupID)] = ans;
	}
	
	public void setPreviousAnswer (int groupID, String ans) {
		prevAnswer[getGroupNo(groupID)] = ans;
	}
	
	public ConcurrentHashMap<Integer, GroupInfoObject> getGroupStats () {
		return groupStatistics;
	}
	
	public boolean setGroupStats(int groupID, GroupInfoObject gio) {
		try {
			groupStatistics.put(groupID, gio);
			return true;
		} catch (Error e) {
			return false;
		}
	}

}
