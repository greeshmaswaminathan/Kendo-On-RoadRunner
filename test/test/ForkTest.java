package test;

import org.junit.Test;

public class ForkTest extends BaseTest {

	@Test
	public void execute() throws InterruptedException {
		DummyESO dummyEso = new DummyESO();
		dummyEso.exec();
		Thread job1 = forkAndAccess(dummyEso);
		job1.join();
		assertNoViolation();
	}
}
