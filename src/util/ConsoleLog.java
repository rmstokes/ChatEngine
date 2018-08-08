package util;

import java.sql.Timestamp;

public class ConsoleLog {
	public static void consoleLog(String s) {
		System.out.println(new Timestamp(System.currentTimeMillis()) + ": " + s);
	}
	
}
