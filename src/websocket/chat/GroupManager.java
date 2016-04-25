package websocket.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import util.CHAT_COLOR;
import util.GroupInfoObject;

public class GroupManager {
	/*
	 * Wrapper for the groups- manages the group info, chat history and colours; etc
	 */
	private static final Log log = LogFactory.getLog(FileLogger.class);
	
	public final String instructor;
	public Date timeOfGroupCreation;
	public final int groupOffset;
	public static CHAT_COLOR TA_COLOR = CHAT_COLOR.Red;
	public static CHAT_COLOR TA2_COLOR = CHAT_COLOR.Tomato;
	public static CHAT_COLOR TA3_COLOR = CHAT_COLOR.Crimson;
	public static CHAT_COLOR SERVER_COLOR = CHAT_COLOR.Black;
	
	//List of Clients for each group
	//ConcurrentHashMap instead of HashTable cause the latter is deprecated
	//CopyOnWriteArraySet since HashSet is less efficient for high number of reads
	//CopyOnWrite also means that broadcasts dont need to be synchronized
	private ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> > groupTable = new ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> >();
	
	//List of AdminMonitors and their information
	private ConcurrentHashMap<Client, Boolean[]> adminMonitorTable = new ConcurrentHashMap<Client, Boolean[]>();
	private ConcurrentHashMap<Integer, GroupInfoObject> groupStatistics = new ConcurrentHashMap<Integer, GroupInfoObject>();
	
	public GroupManager(int numGroups, int groupOffset) {
		this(numGroups, "Unknown_Instructor", groupOffset);
	}
	
	public GroupManager(int numGroups, String instructor, int groupOffset) {
		System.out.println("Group Manager Constructor: Num:"+numGroups + " Group Offset:"+groupOffset +" Instructor: "+instructor); 
		
		//Populate group list
		for (int key = 1; key <= numGroups; key++) {
			//HashSet<Client> value = new HashSet<Client>();
			CopyOnWriteArraySet<Client> value = new CopyOnWriteArraySet<Client>();
			groupTable.put(key, value);
			groupStatistics.put(key, new GroupInfoObject(key));
		}
		
		this.instructor = instructor;
		this.groupOffset = groupOffset;
		
		timeOfGroupCreation = new Date();
		
	}
	
	public boolean joinGroup(int groupNo, Client user) {
		Set<Client> group = getGroup(groupNo);
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
	public Set<Client> getGroup(int groupNo) {
		return groupTable.get(groupNo-groupOffset);
	}
	
	public int getNumOfGroups() {
		return groupTable.size();
	}
	
	public int getGroupOffID(int groupID) {
		return groupID + groupOffset;
	}
	
	//destroy
	public boolean destroy() {
		//remove all clients and set groupID to -1
		for (int i=0; i<getNumOfGroups(); i++) {
			Set<Client> group = groupTable.get(i+1);
			//if (group.size()==0) continue;
			for (Client uc : group) {
				uc.groupID = -1; //Not in a group
				group.remove(uc);
				//redirect to login
				try {
					String xml;
					if (uc.isAdmin)
						xml = "<message type='redirect' path='adminMonitor' senderID='" + uc.permID +"'></message>";
					else
						xml = "<message type='redirect' path='login' senderID='" + uc.permID +"'></message>";
					uc.session.getBasicRemote().sendText(xml);
					//uc.session.close(); //Attempt to close the session
				} catch (Exception e) {
					System.out.println("Could not remove user from closing group");
				}
				//String msg =  "<message type='redirect' path='login' senderID='" + uc.permID +"'></message>";
				//uc.session.getBasicRemote().sendText(msg);
			}
		}
		
		//AdminMonitors should already be dropped from each group
		//Remove adminStatus
		//for (Client userClient : adminMonitorTable.keySet())
			//userClient.isAdmin = false;
		
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
		int groupNum = this.getNumOfGroups();
		Boolean[] monitorTable = new Boolean[groupNum*2]; //2 slots per group
		for (int i=0; i<monitorTable.length; i++)
			monitorTable[i] = false;
		
		try {
			adminMonitorTable.put(userClient, monitorTable);
			userClient.isAdmin = true;
			return adminMonitorTable.containsKey(userClient);
		} catch (Error e) {
			log.error(e);
			return false;
		}
	}
	
	public boolean checkIfAdminMonitor(Client userClient) {
		return adminMonitorTable.containsKey(userClient);
	}
	
	//boolean 0=monitor, 1=chat
	public Boolean[] getAMStatus(Client userClient) {
		return adminMonitorTable.get(userClient);
	}
	
	public Boolean[] setAMStatus(Client user, Boolean[] AMStatus) {
		return adminMonitorTable.put(user, AMStatus);
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
