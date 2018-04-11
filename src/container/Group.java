package container;

public class Group {
	private String groupId;
	private int completedQuestions = 0;
	
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

}
