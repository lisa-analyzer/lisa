package it.unive.lisa;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.List;

public class TestHeapDomain extends TestDomain<TestHeapDomain, SymbolicExpression>
		implements HeapDomain<TestHeapDomain> {

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("heap");
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public ExpressionSet rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new ExpressionSet();
	}
}
