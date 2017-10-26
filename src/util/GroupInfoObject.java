package util;

import java.util.HashMap;

//This is an info object that contains statistics for each group
//Hopefully this can be expanded later: an idea is to have a list
//of each message from each person, and chart that information.
public class GroupInfoObject {

	//How up to date this object is compared to the total number of messages logs
	public int fileCounter = 0;
	//Name of group
	public int groupID;
	
	//Statistics per user
	public HashMap<String, Integer[]> groupMap = new HashMap<String, Integer[]>();
	//String is the name of the user
	//Integer has 3 values-
	//1st is the number of typing messgaes
	//2nd is the number of chat messages
	//3rd is the participation number (based on an algorithm)
	
	public Integer[] totalStats = {0, 0};
	//Total number of stats (ignore participation)
	
	public GroupInfoObject(int groupID) {
		this.groupID = groupID;
	}
	
}
