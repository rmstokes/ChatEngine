package container;

import java.util.HashMap;
import java.util.Set;

public class dashStatsContainer {
	
	private HashMap<String, Group> groups = new HashMap<String, Group>();
	private int qCount;
	private String path = "";
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	private dashStatsContainer() {}
	
	private static class LazyHolder{
		static final dashStatsContainer INSTANCE = new dashStatsContainer();
	}
	
	public static dashStatsContainer getInstance() {
		return LazyHolder.INSTANCE;
	}
	
	public void setQCount(int count) {
		this.qCount = count;
		//System.out.println("qCount: " + this.qCount);
	}
	public int getQCount() {
		return this.qCount;
	}
//	public HashMap<String, Group> getGroupsHashmap() {
//		return this.groups;
//	}
	public void initializeGroup(String id) {
		System.out.println("Initialized group with String " + id + " in dashStatsContainer");
		groups.put(id, new Group(id));
	}
	public void initializeGroup(int id) {
		System.out.println("Initialized group with int " + id + " in dashStatsContainer");
		groups.put(cleanID(id), new Group(cleanID(id)));
	}	
	public Set<String> getGroupKeys(){
		return groups.keySet();
	}
//	public void setGroupCorrectQs(int id, int correctCount) {
//		//groups[id] = correct
//		
//	}
	public int getGroupCorrectQs(String id) {
		
		return groups.get(id).getComplQs();
		
	}
	public int getGroupCorrectQs(int id) {
		return groups.get(cleanID(id)).getComplQs();
		
	}
	
	private String cleanID(int i) {
		String result = (i < 10) ? "0"+i : ""+i;	
		return result;
	}
	private void printKeys() {
		for (String key : getGroupKeys()) {
			System.out.println("key: "+ key);
		}
		
	}
	
	public void incrementCQs(String id) {
		groups.get(id).incrementCQs();
	}
	public void incrementCQs(int id) {
		groups.get(cleanID(id)).incrementCQs();
	}
	public void decrementCQs(String id) {
		if (groups.get(id).getComplQs() > 0) {
			groups.get(id).decrementCQs();
		}
	}
	public void decrementCQs(int id) {
		if (groups.get(cleanID(id)).getComplQs() > 0) {
			groups.get(cleanID(id)).decrementCQs();
		}
	}
	public void addGroupAM(int id, String uName) {
		String stringID = cleanID(id);
		
		System.out.println("Added " + uName + " to '" + stringID + "'");
		
		groups.get(stringID).addAM(uName);
	}
	public void removeGroupAM(int id, String uName) {
		String stringID = cleanID(id);
		
		System.out.println("Removed " + uName + " from '" + stringID + "'");
		
		groups.get(stringID).removeAM(uName);
	}
	public Set<String> getAMs(String key){
		return groups.get(key).getAMs();
		
		
	}
	
}
