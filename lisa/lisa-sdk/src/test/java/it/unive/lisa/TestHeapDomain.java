package it.unive.lisa;

import java.util.List;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class TestHeapDomain extends BaseHeapDomain<TestHeapDomain> {

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return null;
	}

	@Override
	public TestHeapDomain assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return null;
	}

	@Override
	public TestHeapDomain assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestHeapDomain forgetIdentifier(Identifier id) throws SemanticException {
		return null;
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public DomainRepresentation representation() {
		return null;
	}

	@Override
	public TestHeapDomain top() {
		return null;
	}

	@Override
	public TestHeapDomain bottom() {
		return null;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return null;
	}

	@Override
	protected TestHeapDomain mk(TestHeapDomain reference) {
		return null;
	}

	@Override
	protected TestHeapDomain semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	protected TestHeapDomain lubAux(TestHeapDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected TestHeapDomain wideningAux(TestHeapDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(TestHeapDomain other) throws SemanticException {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
