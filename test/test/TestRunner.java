package test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class TestRunner {

	public static void main(String[] args) {
		JUnitCore junit = new JUnitCore();
	    junit.addListener(new TextListener(System.err));
	    Result r = junit.run(ForkTest.class, JoinTest.class, LockTest.class, RaceTest.class, UnsyncTest.class);
		System.exit(r.wasSuccessful() ? 0 : 1);
	}
}
