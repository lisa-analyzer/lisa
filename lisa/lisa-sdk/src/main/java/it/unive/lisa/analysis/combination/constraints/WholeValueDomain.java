package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Set;

public interface WholeValueDomain<D extends WholeValueDomain<D>>
		extends
		BaseNonRelationalValueDomain<D> {

	Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException;

	D generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException;
}
