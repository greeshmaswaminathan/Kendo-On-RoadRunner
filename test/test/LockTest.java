package test;

import org.junit.Test;

public class LockTest extends BaseTest {

	private Object lock = new Object();
	private DummyESO dummyEso;

	private class Job1 implements Runnable {
		@Override
		public void run() {
			synchronized (lock) {
				dummyEso.exec();
			}
		}
	}

	@Test
	public void execute() throws InterruptedException {
		dummyEso = new DummyESO();
		dummyEso.exec();
		Thread t1 = new Thread(new Job1());
		t1.start();
		synchronized (lock) {
			dummyEso.exec();
		}
		t1.join();
		assertNoViolation();

	}

}
