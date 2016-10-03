package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import rr.error.ErrorMessage;

public class BaseTest {

	public BaseTest() {
		super();
	}
	
	protected Thread forkAndAccess(final DummyESO eso) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				eso.exec();
			}
		});
		t.start();
		return t;
	}

	
	int previousErrors = 0;
	
	@Before
	public void resetViolations() {
		previousErrors = ErrorMessage.getTotalNumberOfErrors();
	}
	
	protected void assertNoViolation() {
		assertEquals(previousErrors, ErrorMessage.getTotalNumberOfErrors());
	}

	protected void assertViolation() {
		assertTrue(previousErrors < ErrorMessage.getTotalNumberOfErrors());
	}
}