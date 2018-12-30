package rebar.util;

import java.util.concurrent.TimeUnit;

public class Sleep {

	public static void sleep(int duration, TimeUnit unit) {
		sleep(unit.toMillis(duration));
	}
	public static void sleep(int ts) {
		sleep((long)ts);
	}
	
	public static void sleep(long ts) {
		try {
			Thread.sleep(ts);
		}
		catch (InterruptedException ignore) {
			// ignore
		}
	}

}
