package it.unive.lisa.analysis.heap;

import java.util.Collections;
import java.util.List;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;

public class MonolithicHeap extends BaseLattice<MonolithicHeap> implements HeapDomain<MonolithicHeap> {

	private static final MonolithicHeap TOP = new MonolithicHeap();

	private static final MonolithicHeap BOTTOM = new MonolithicHeap();
	
	private static final HeapIdentifier MONOLITH = new HeapIdentifier("heap");

	private final ValueExpression rewritten;
	
	public MonolithicHeap() {
		rewritten = new Skip();
	}
	
	private MonolithicHeap(ValueExpression rewritten) {
		this.rewritten = rewritten;
	}
	
	@Override
	public ValueExpression getRewrittenExpression() {
		return rewritten;
	}

	@Override
	public List<Replacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public MonolithicHeap assign(Identifier id, SymbolicExpression expression) throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression);
	}

	@Override
	public MonolithicHeap smallStepSemantics(SymbolicExpression expression) throws SemanticException {
		if (expression instanceof HeapReference)
			return new MonolithicHeap(MONOLITH);
		
		// TODO fill this when the symbolic expression structure will be filled in
		
		if (expression instanceof ValueExpression)
			return new MonolithicHeap((ValueExpression) expression);
		
		return TOP;
	}

	@Override
	public MonolithicHeap assume(SymbolicExpression expression) throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public MonolithicHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new MonolithicHeap();
	}

	@Override
	protected MonolithicHeap lubAux(MonolithicHeap other) throws SemanticException {
		checkExpression(other);
		return this;
	}

	@Override
	protected MonolithicHeap wideningAux(MonolithicHeap other) throws SemanticException {
		checkExpression(other);
		return this;
	}

	@Override
	protected boolean lessOrEqualAux(MonolithicHeap other) throws SemanticException {
		checkExpression(other);
		return true;
	}
	
	private void checkExpression(MonolithicHeap other) throws SemanticException {
		// TODO we want to eventually support this
		if (!rewritten.equals(other.rewritten))
			throw new SemanticException(
					"Semantic operations on instances with different expressions is not yet supported");
	}

	@Override
	public MonolithicHeap top() {
		return TOP;
	}

	@Override
	public MonolithicHeap bottom() {
		return BOTTOM;
	}
}
