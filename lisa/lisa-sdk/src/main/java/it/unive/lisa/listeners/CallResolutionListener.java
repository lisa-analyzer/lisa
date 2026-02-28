package it.unive.lisa.listeners;

import it.unive.lisa.ReportingTool;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.interprocedural.callgraph.events.CallResolved;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.ResolvedCall;
import java.util.stream.Collectors;

/**
 * An event listener that issues notices on call resolution events.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallResolutionListener
		implements
		EventListener {

	private final boolean noticeAll;

	/**
	 * Builds the listener that only notices open calls.
	 */
	public CallResolutionListener() {
		this(false);
	}

	/**
	 * Builds the listener.
	 * 
	 * @param noticeAll if {@code true}, notices are issued for all call
	 *                      resolutions, otherwise only open calls are noticed
	 */
	public CallResolutionListener(
			boolean noticeAll) {
		this.noticeAll = noticeAll;
	}

	@Override
	public void onEvent(
			Event event,
			ReportingTool tool) {
		if (!(event instanceof CallResolved))
			return;
		CallResolved cr = (CallResolved) event;
		if (cr.getResolved() instanceof OpenCall)
			tool.noticeOn(cr.getOriginal(), "The call at "
					+ cr.getOriginal().getLocation()
					+ " was not resoolved to any target and is thus open");
		else if (noticeAll)
			tool.noticeOn(cr.getOriginal(), "The call at "
					+ cr.getOriginal().getLocation()
					+ " was resolved to "
					+ ((ResolvedCall) cr.getResolved()).getTargets().stream()
							.map(t -> t.getDescriptor().getFullSignature())
							.sorted()
							.collect(Collectors.joining(", ")));
	}

}
