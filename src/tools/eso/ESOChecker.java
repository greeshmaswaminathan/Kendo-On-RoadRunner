package tools.eso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.option.CommandLine;
import rr.annotations.Abbrev;
import rr.contracts.ThreadUnsafe;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AcquireEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.meta.ClassInfo;
import rr.meta.MethodInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.tool.Tool;
import tools.util.CV;

@Abbrev("ESO")
public class ESOChecker extends Tool {
	
	private static final int CV_INIT_SIZE = 2;

	private static final ErrorMessage<MethodInfo> violations = ErrorMessages.makeMethodErrorMessage("ESOViolations");
	
	private static Collection<ClassInfo> classes;

	private final Map<Object, CV> esoData = new WeakHashMap<>();
	
	private final Decoration<ShadowLock, CV> esoLockData = makeLockDecoration("ESO:ShadowLock", new CV(CV_INIT_SIZE));

	static CV ts_get_cv(ShadowThread ts) {
		Assert.panic("Bad");
		return null;
	}

	static void ts_set_cv(ShadowThread ts, CV cv) {
		Assert.panic("Bad");
	}

	public ESOChecker(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		classes = new ArrayList<>();
	}

	private boolean isThreadUnsafe(ClassInfo c) {
		if (classes.contains(c)) {
			return true;
		}
		if (c.hasAnnotation(ThreadUnsafe.class)) {
			classes.add(c);
			return true;
		}
		return false;
	}

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread ct = e.getThread();
		CV cv = new CV(CV_INIT_SIZE);
		ShadowThread pt = ct.getParent();
		if (pt != null) {
			CV pcv = ts_get_cv(pt);
			cv.max(pcv);
			pcv.inc(pt.getTid());
		}
		cv.inc(ct.getTid());
		ts_set_cv(ct, cv);
		super.create(e);
	}

	@Override
	public void postJoin(JoinEvent je) {
		CV ctcv = ts_get_cv(je.getThread());
		CV jtcv = ts_get_cv(je.getJoiningThread());
		ctcv.max(jtcv);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		final ShadowThread ct = ae.getThread();
		final ShadowLock l = ae.getLock();
		ts_get_cv(ct).max(esoLockData.get(l));
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		final ShadowThread ct = re.getThread();
		final ShadowLock l = re.getLock();
		CV lcv = esoLockData.get(l);
		CV ctcv = ts_get_cv(ct);
		lcv.set(ct.getTid(), ctcv.get(ct.getTid())); // updated the lock vc
		ctcv.inc(ct.getTid()); // incremented the cv value for current thread
		super.release(re);
	}

	@Override
	public void enter(MethodEvent me) {
		ClassInfo esoInfo = me.getInfo().getOwner();
		if (isThreadUnsafe(esoInfo) && isInstance(me)) {
			Object eso = me.getTarget();
			CV esocv = esoData.get(eso);
			if (esocv == null) {
				esocv = new CV(CV_INIT_SIZE);
				esoData.put(eso, esocv);
			}

			ShadowThread ct = me.getThread();
			int cid = ct.getTid();
			CV ctcv = ts_get_cv(ct);
			if (esocv.anyGt(ctcv)) {
				reportContractViolation(me);
			}
			esocv.set(cid, ctcv.get(cid));
			ctcv.inc(cid);
			super.enter(me);
		}
	}

	private boolean isInstance(MethodEvent me) {
		return !me.getInfo().isStatic() && me.getTarget() != null;
	}
	
	private void reportContractViolation(MethodEvent me) {
		Util.printf("\nContract Violation: %s\n", me + "  by thread " + me.getThread().getTid());
		violations.error(me.getThread(), me.getInfo(), "ContractViolated", "ESO");
	}
}
