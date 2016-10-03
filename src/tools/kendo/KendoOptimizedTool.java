package tools.kendo;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.tool.Tool;

@Abbrev("KENDO_OPT")
public class KendoOptimizedTool extends Tool {

	// a map of thread to dterministic logical clock
	// counter incrementing
	// logic on lock acquisition
	// logic on lock release
	// optimizations
	private List<ShadowThread> allThreads = null;
	private Decoration<ShadowLock, Queue<ShadowThread>> fairnessQueue = ShadowLock.makeDecoration("queue",
			DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowLock, Queue<ShadowThread>>() {
				/**
				* 
				*/
				private static final long serialVersionUID = 8746177266853416672L;

				public Queue<ShadowThread> get(ShadowLock st) {
					return new LinkedList<ShadowThread>();
				}
			});

	private Decoration<ShadowThread, Long> logicalClockDecoration = ShadowThread.makeDecoration("lc",
			DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowThread, Long>() {
				/**
				* 
				*/
				private static final long serialVersionUID = 8746177266853416672L;

				public Long get(ShadowThread st) {
					return -1l;
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
	
	private Decoration<ShadowLock, Long> lockReleasedLogicalTimes = ShadowLock.makeDecoration("lockrelease",
			DecorationFactory.Type.MULTIPLE, new DefaultValue<ShadowLock, Long>() {
				/**
				* 
				*/
				private static final long serialVersionUID = 8746177266853416672L;

				public Long get(ShadowLock st) {
					return -1l;
				}
			});
	
	public KendoOptimizedTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		allThreads = new ArrayList<ShadowThread>();
	}

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread thread = e.getThread();
		if (getThreadId(thread) != 0){
			allThreads.add(thread);
			logicalClockDecoration.set(thread, 0L);
		}
			
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

	public boolean testAcquire(AcquireEvent ae) {
		ShadowThread thread = ae.getThread();
		ShadowLock lock = ae.getLock();
		// pause logical clock
		pauseLogicalClock(thread);
		while (true) {
			waitForTurn(thread);
			if(fairnessQueue.get(lock).isEmpty() || thread.equals(fairnessQueue.get(lock).peek())){
				if(isLockFree(thread, lock)){
					fairnessQueue.get(lock).poll();
					break;
				}
			}else{
				incrementLogicalClock(thread, false);
				if(!fairnessQueue.get(lock).contains(thread)){
					fairnessQueue.get(lock).add(thread);
				}
			}
			
		}
		//Util.printf("got turn for " + thread.getTid());
	
		return super.testAcquire(ae);
	}
	
	private boolean isLockFree(ShadowThread thread, ShadowLock lock){
		if (trylock(lock, thread)) {
			Long releasedLogicalTime = getReleasedLogicalTime(lock);
			Long logicalClock = getLogicalClock(thread);
			if (releasedLogicalTime < logicalClock) {
				return true;
			} else {
				//Util.printf("Lock still held in logical time " + releasedLogicalTime +" , "+ logicalClock);
				setLogicalClock(thread, false, releasedLogicalTime+1);
			}
		}else{
			//Util.printf("Try lock failed for thread "+thread);
			incrementLogicalClock(thread, false);
			if(!fairnessQueue.get(lock).contains(thread)){
				fairnessQueue.get(lock).add(thread);
			}
			
		}
		return false;
	}
	
	@Override
	public void acquire(AcquireEvent ae) {
		ShadowThread thread = ae.getThread();
		incrementLogicalClock(thread, false);
		// resume logical clock
		resumeLogicalClock(thread);
		super.acquire(ae);
	}

	

	private void waitForTurn(ShadowThread current) {
		//Util.printf("enter wait for turn "+current);
		Long currentThreadClock = logicalClockDecoration.get(current);
		//Util.printf("waiting for turn of " + current.getTid() + " with clock :" + (currentThreadClock));

		while (true) {
			boolean isTurn = true;
			Iterator<ShadowThread> iterator = allThreads.iterator();
			while (iterator.hasNext()) {
				ShadowThread thread = iterator.next();
				Long threadClock = logicalClockDecoration.get(thread);
				if(thread.getThread().getState() != State.TERMINATED){
					if (getThreadId(thread) < getThreadId(current) && !(threadClock > currentThreadClock)) {
						//Util.printf("found a smaller thread " + thread.getTid() +" with clock :" + threadClock);
							isTurn = false;
							break;
						} else if (getThreadId(thread) > getThreadId(current) && !(threadClock >= currentThreadClock)) {
							//Util.printf("found a bigger thread " + thread.getTid() +" with clock :" + threadClock);
							isTurn = false;
							break;
						}
				}
				
			}
			if (isTurn) {
				//Util.printf("exit wait for turn "+current);
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
	public boolean testRelease(ReleaseEvent re) {
		ShadowThread thread = re.getThread();
		pauseLogicalClock(thread);
		setLockReleasedLogicalTime(re.getLock(), getLogicalClock(thread));
		return super.testRelease(re);
	}

	@Override
	public void release(ReleaseEvent re) {
		ShadowThread thread = re.getThread();
		incrementLogicalClock(thread, false);
		resumeLogicalClock(thread);
		
		super.release(re);
	}

	private void setLockReleasedLogicalTime(ShadowLock lock, Long lc) {
		lockReleasedLogicalTimes.set(lock, lc);
	}

	private Long incrementLogicalClock(ShadowThread thread, boolean pauseCheck) {
		Long clock = logicalClockDecoration.get(thread);
		return setLogicalClock(thread, pauseCheck, clock+1);
	}
	
	private Long setLogicalClock(ShadowThread thread, boolean pauseCheck, long newValue) {
		Long clock = logicalClockDecoration.get(thread);
		if(pauseCheck){
			if (!pausedStatusDecoration.get(thread)) {
				logicalClockDecoration.set(thread, newValue);
			}
		}else{
			logicalClockDecoration.set(thread, newValue);
		}
		
		return clock;
	}


	private Long getLogicalClock(ShadowThread thread) {
		return logicalClockDecoration.get(thread);
	}

	private void pauseLogicalClock(ShadowThread thread) {
		pausedStatusDecoration.set(thread, true);
	}

	private void resumeLogicalClock(ShadowThread thread) {
		pausedStatusDecoration.set(thread, false);
	}
	
	private Long getReleasedLogicalTime(ShadowLock lock) {
		return lockReleasedLogicalTimes.get(lock);
	}

	private boolean trylock(ShadowLock lock, ShadowThread thread) {
		Util.printf("lock.getHoldingThread():"+lock.getHoldingThread()+" and current thread "+thread);
		/*try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		if (lock.getHoldingThread() == null || lock.getHoldingThread() == thread)
			return true;
		return false;
	}
}
