package test;

import org.junit.Test;

public class UnsyncTest extends BaseTest {

	@Test
	public void execute() throws InterruptedException {
		DummyESO dummyEso = new DummyESO();
		Thread job1 = forkAndAccess(dummyEso);
		dummyEso.exec();
		job1.join();
		assertViolation();
	}

}
