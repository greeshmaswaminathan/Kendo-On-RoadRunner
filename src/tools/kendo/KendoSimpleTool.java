package tools.kendo;

import java.lang.Thread.State;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.NewThreadEvent;
import rr.state.ShadowThread;
import rr.tool.Tool;

@Abbrev("KENDO_SIMPLE")
public class KendoSimpleTool extends Tool {

	private List<ShadowThread> allThreads = null;

	private Decoration<ShadowThread, Long> logicalClockDecoration = ShadowThread.makeDecoration("lc",
			DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowThread, Long>() {
				/**
				* 
				*/
				private static final long serialVersionUID = 8746177266853416672L;

				public Long get(ShadowThread st) {
					return 0l;
				}
			});
	private Decoration<ShadowThread, Boolean> pausedStatusDecoration = ShadowThread.makeDecoration("paused",
			DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowThread, Boolean>() {
				/**
				* 
				*/
				private static final long serialVersionUID = 8746177266853416672L;

				public Boolean get(ShadowThread st) {
					return false;
				}
			});

	public KendoSimpleTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		allThreads = new CopyOnWriteArrayList<ShadowThread>();
	}

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread thread = e.getThread();
		if (getThreadId(thread) != 0)
			allThreads.add(thread);
		super.create(e);
	}

	@Override
	public void access(AccessEvent fae) {

		ShadowThread thread = fae.getThread();
		Long clock = incrementLogicalClock(thread, true);
		// Util.printf("logical clock value for " + thread.getTid() + " is :" +
		// (clock + 1));
		super.access(fae);
	}

	private Long incrementLogicalClock(ShadowThread thread, boolean pauseCheck) {
		Long clock = logicalClockDecoration.get(thread);
		if (pauseCheck) {
			if (!pausedStatusDecoration.get(thread)) {
				logicalClockDecoration.set(thread, clock + 1);
			}
		} else {
			logicalClockDecoration.set(thread, clock + 1);
		}

		return clock;
	}

	@Override
	public boolean testAcquire(AcquireEvent ae) {
		// Util.printf("enter test Acquire for "+ae.getThread().getTid());
		ShadowThread thread = ae.getThread();
		pausedStatusDecoration.set(thread, true);
		waitForTurn(thread);
		Util.printf("got turn for "+getThreadId(thread));

		return super.testAcquire(ae);

	}

	@Override
	public void acquire(AcquireEvent ae) {

		ShadowThread thread = ae.getThread();
		incrementLogicalClock(thread, false);
		pausedStatusDecoration.set(thread, false);
		//Util.printf("************" + ae.getThread().getThread().holdsLock(ae.getLock().getLock()));
		super.acquire(ae);
	}

	private void waitForTurn(ShadowThread current) {
		Long currentThreadClock = logicalClockDecoration.get(current);
		// Util.printf("waiting for turn of " + current.getTid() + " with clock
		// :" + (currentThreadClock));

		while (true) {
			boolean isTurn = true;
			Iterator<ShadowThread> iterator = allThreads.iterator();
			while (iterator.hasNext()) {
				ShadowThread thread = iterator.next();
				Long threadClock = logicalClockDecoration.get(thread);
				if(thread.getThread().getState() != State.TERMINATED){
				if (getThreadId(thread) < getThreadId(current) && !(threadClock > currentThreadClock)) {
					// Util.printf("found a smaller thread " + thread.getTid()
					// +" with clock :" + threadClock);
					isTurn = false;
					break;
				} else if (getThreadId(thread) > getThreadId(current) && !(threadClock >= currentThreadClock)) {
					// Util.printf("found a bigger thread " + thread.getTid() +"
					// with clock :" + threadClock);
					isTurn = false;
					break;
				}
			 }

			}
			if (isTurn) {
				// Util.printf("exit wait for turn "+current);
				return;
			}
		}

	}

	private int getThreadId(ShadowThread thread) {
		String name = thread.getThread().getName();
		if("main".equals(name)) {
			return 0;
		}else{
			return Integer.parseInt(name);
		}
	}


	@Override
	public void stop(ShadowThread td) {
		allThreads.remove(td);
		super.stop(td);
	}
}
