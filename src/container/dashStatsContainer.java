package container;

public class dashStatsContainer {
	
	private Group [] groups;
	private int qCount;
	
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
//	public setGroups(int )
}
