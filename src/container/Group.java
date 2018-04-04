package container;

public class Group {
	private int groupId;
	private int completedQuestions = 0;
	
	public Group(int id){
		this.groupId = id;
	}
	
	public int getGroupId() {
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

}
