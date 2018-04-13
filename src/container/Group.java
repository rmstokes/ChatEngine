package container;

import java.util.HashSet;
import java.util.Set;

public class Group {
	private String groupId;
	private int completedQuestions = 0;
	private Set<String> setAMs = new HashSet<String>();
	private Set<String> setUNames = new HashSet<String>();
	
	public Set<String> getSetUNames() {
		return setUNames;
	}

	public void addUname(String s) {
		this.setUNames.add(s);
	}
	public void removeUname(String s) {
		if(this.setUNames.contains(s))
			this.setUNames.remove(s);
	}

	public Group(String id){
		this.groupId = id;
	}
	
	public String getGroupId() {
		return this.groupId;
	}
	public void incrementCQs() {
		this.completedQuestions++;
	}
	public void decrementCQs() {
		if (this.completedQuestions > 0) {
			this.completedQuestions--;
		}
	}
	public int getComplQs() {
		return this.completedQuestions;
	}
	public void addAM(String s) {
		setAMs.add(s);
	}
	public void removeAM(String s) {
		if (setAMs.contains(s)) {
			setAMs.remove(s);
		}
	}
	public Set<String> getAMs(){
		return setAMs;
		
	}
	

}
