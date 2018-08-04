package container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class dashStatsContainer {
	
	private HashMap<String, Group> groups = new HashMap<String, Group>();
	private HashMap<String, String> MD5Map = new HashMap<String, String>(); 
	private int qCount;
	private String path = "";
	//private String masterLogPath = "";
	
//	public String getMasterLogPath() {
//		return masterLogPath;
//	}
//
//	public void setMasterLogPath(String masterLogPath) {
//		this.masterLogPath = masterLogPath;
//		MD5Map = importMD5Map();
//	}

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
	public Set<String> getSetUNames(String groupID) {
		return groups.get(groupID).getSetUNames();
	}

	public void addUname(int id, String s) {
		String stringID = cleanID(id);
		groups.get(stringID).addUname(s);
	}
	public void removeUname(int id, String s) {
		String stringID = cleanID(id);
		groups.get(stringID).removeUname(s);
	}
	public void clearUNames() {
		groups.clear();
	}
	public String saveMD5hash(String plaintext) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(plaintext.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		String hashtext = bigInt.toString(16);
		// Now we need to zero pad it if you actually want the full 32 chars.
		while(hashtext.length() < 32 ){
		  hashtext = "0"+hashtext;
		}
		if (!MD5Map.containsKey(hashtext)) {
			MD5Map.put(hashtext, plaintext);
		}
		
		//saveMD5Map();
		return hashtext;
	}
	// disallow collisions
	public String getNameFromMD5(String key) {
		return MD5Map.get(key);
		
	}
	
	public void resetMD5Map() {
		MD5Map.clear();
	}
	
	synchronized public void saveMD5Map() {
		if (path == "") {
			// not enough time has passed for a path to be generated
			// can't save
			return;
		}
		String fileOutPath = path + "users.txt";
		
		try {
	         File fileOut =
	         new File(fileOutPath);
	         
	         //makes clean file
	         if (fileOut.exists()) fileOut.delete();
	         
	         FileWriter out = new FileWriter(fileOut);	     
	         
	         Iterator<Map.Entry<String, String>> it = MD5Map.entrySet().iterator();
	         while (it.hasNext()) {
	        	 Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        	 out.append(pair.getKey() + " " + pair.getValue() + System.lineSeparator());
	        	 //it.remove();
	         }
	         
	         out.close();
	        
	      } catch (IOException i) {
	         i.printStackTrace();
	      }
	}
	
	/*
	 * importMD5 map is not used because there should be no reason to get a map 
	 * from a previous session. A python tool will be built to rebuild log files 
	 */
//	private HashMap<String, String> importMD5Map(){
//		HashMap<String, String> importMap = null;
//		
//		try {
//	         FileInputStream fileIn = new FileInputStream(path + "users.txt");
//	         ObjectInputStream in = new ObjectInputStream(fileIn);
//	         importMap = (HashMap<String, String>) in.readObject();
//	         in.close();
//	         fileIn.close();
//	      } catch (IOException i) {
//	         i.printStackTrace();
//	         return null;
//	      } catch (ClassNotFoundException c) {
//	         System.out.println("MD5Map class not found");
//	         c.printStackTrace();
//	         return null;
//	      }
//		
//		return importMap;
//		
//	}
	
}
