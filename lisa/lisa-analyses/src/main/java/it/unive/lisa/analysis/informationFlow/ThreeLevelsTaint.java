package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.lattices.informationFlow.ThreeTaint;

/**
 * A {@link BaseTaint} implementation with three level of taintedness: clean,
 * tainted and top. As such, this class distinguishes values that are always
 * clean, always tainted, or tainted in at least one execution path.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ThreeLevelsTaint
		extends
		BaseTaint<ThreeTaint> {

	@Override
	public ThreeTaint top() {
		return ThreeTaint.TOP;
	}

	@Override
	public ThreeTaint bottom() {
		return ThreeTaint.BOTTOM;
	}

	@Override
	protected ThreeTaint tainted() {
		return ThreeTaint.TAINTED;
	}

	@Override
	protected ThreeTaint clean() {
		return ThreeTaint.CLEAN;
	}

}
