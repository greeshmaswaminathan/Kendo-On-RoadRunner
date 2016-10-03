package test;

import rr.contracts.ThreadUnsafe;

@ThreadUnsafe
public class DummyESOWithVariable {
	private int count = 0;

	public void inc() {
		count = get() + 1;
	}

	public int get() {
		return count;
	}
}