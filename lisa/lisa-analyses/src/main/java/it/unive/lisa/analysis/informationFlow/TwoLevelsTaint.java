package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.lattices.informationFlow.SimpleTaint;

/**
 * A {@link BaseTaint} implementation with only two level of taintedness: clean
 * and tainted. As such, this class distinguishes values that are always clean
 * from values that are tainted in at least one execution path.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TwoLevelsTaint extends BaseTaint<SimpleTaint> {

	@Override
	public SimpleTaint top() {
		return SimpleTaint.TAINTED;
	}

	@Override
	public SimpleTaint bottom() {
		return SimpleTaint.BOTTOM;
	}

	@Override
	protected SimpleTaint tainted() {
		return SimpleTaint.TAINTED;
	}

	@Override
	protected SimpleTaint clean() {
		return SimpleTaint.CLEAN;
	}

}
