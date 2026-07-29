package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis;
import it.unive.lisa.lattices.informationFlow.SimpleTaint;

/**
 * A {@link BaseTaint} implementation with only two level of taintedness: clean
 * and tainted. As such, this class distinguishes values that are always clean
 * from values that are tainted in at least one execution path. <br/>
 * <br/>
 * As an information flow analysis, this domain does not take part in
 * {@link WholeValueAnalysis}, meaning that it will never generate constraints
 * when asked to.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TwoLevelsTaint
		extends
		BaseTaint<SimpleTaint> {

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
