package websocket.chat;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import util.CHAT_COLOR;

public class GroupManager {
	/*
	 * Wrapper for the groups- manages the group info, chat history and colours; etc
	 */
	private static final Log log = LogFactory.getLog(FileLogger.class);
	
	public String instructor;
	public Date timeOfGroupCreation;
	public static CHAT_COLOR TA_COLOR = CHAT_COLOR.Red;
	public static CHAT_COLOR SERVER_COLOR = CHAT_COLOR.Black;
	
	//List of Clients for each group
	//ConcurrentHashMap instead of HashTable cause the latter is deprecated
	//CopyOnWriteArraySet since HashSet is less efficient for high number of reads
	//CopyOnWrite also means that broadcasts dont need to be synchronized
	private ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> > groupTable = new ConcurrentHashMap<Integer, CopyOnWriteArraySet<Client> >();
	
	public GroupManager(int numGroups) {
		this(numGroups, "Unknown Instructor");
	}
	
	public GroupManager(int numGroups, String instructor) {
		System.out.println("Group Manager constructor: no - "+numGroups + " Instructor: "+instructor); 
		
		//Populate group list
		for (int key = 1; key <= numGroups; key++) {
			//HashSet<Client> value = new HashSet<Client>();
			CopyOnWriteArraySet<Client> value = new CopyOnWriteArraySet<Client>();
			groupTable.put(key, value);
		}
		
		this.instructor = instructor;
		
		timeOfGroupCreation = new Date();
		
	}
	
	public boolean joinGroup(int groupNo, Client user) {
		//HashSet<Client> group = groupTable.get(groupNo);
		CopyOnWriteArraySet<Client> group = groupTable.get(groupNo);
		try {
			return group.add(user);
		} catch (Error e) {
			log.error(e);
			return false;
		}
		
	}
	
	public boolean assignChatColor(Client user) {
		//if (user==null) return false;
		if (user.username.startsWith("TA") ) {
			//Will later have a better identifier if I add a password feature
			//and client a privilege variable
			user.chatColor = TA_COLOR;
			return true;
		}
		
		//Choose color value at random
		CHAT_COLOR[] chatColorArr = CHAT_COLOR.values();
		boolean[] recordArr = new boolean[chatColorArr.length];
		//HashSet<Client> group = groupTable.get(user.groupID);
		Set<Client> group = groupTable.get(user.groupID);
		
		for (Client groupMem : group) { //get currently used color values
			if (groupMem==user) continue;
			recordArr[groupMem.chatColor.ordinal()] = true;
		}
		//exclude TA color & Server color
		recordArr[TA_COLOR.ordinal()] = true;
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
	
	//public synchronized HashSet<Client> getGroup(int groupNo) {
	public Set<Client> getGroup(int groupNo) {
		return groupTable.get(groupNo);
	}
	
	public int getNumOfGroups() {
		return groupTable.size();
	}
	

}
