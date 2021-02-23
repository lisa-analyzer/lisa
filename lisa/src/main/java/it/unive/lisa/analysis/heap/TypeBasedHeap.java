package it.unive.lisa.analysis.heap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import it.unive.lisa.analysis.BaseHeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

public class TypeBasedHeap extends BaseHeapDomain<TypeBasedHeap> {

	private static final TypeBasedHeap TOP = new TypeBasedHeap();

	private static final TypeBasedHeap BOTTOM = new TypeBasedHeap();

	private final Collection<ValueExpression> rewritten;

	private static HashSet<String> NAMES = new HashSet<String>();

	public TypeBasedHeap() {
		this(new Skip());
	}

	private TypeBasedHeap(ValueExpression rewritten) {
		this(Collections.singleton(rewritten));
	}

	private TypeBasedHeap(Collection<ValueExpression> rewritten) {
		this.rewritten = rewritten;
	}

	@Override
	public TypeBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public TypeBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public TypeBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new TypeBasedHeap(rewritten);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		return NAMES.toString();
	}

	@Override
	public TypeBasedHeap top() {
		return TOP;
	}

	@Override
	public TypeBasedHeap bottom() {
		return BOTTOM;
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	protected TypeBasedHeap mk(TypeBasedHeap reference, ValueExpression expression) {
		return new TypeBasedHeap(expression);
	}

	@Override
	protected TypeBasedHeap semanticsOf(HeapExpression expression) {
		HashSet<ValueExpression> ids = new HashSet<>();
		for (Type type : expression.getTypes()) {
			if (type.isPointerType()) {
				ids.add(new HeapIdentifier(Caches.types().mkSingletonSet(type), type.toString(), true));
				NAMES.add(type.toString());
			}
		}

		return new TypeBasedHeap(ids);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected TypeBasedHeap lubAux(TypeBasedHeap other) throws SemanticException {
		return new TypeBasedHeap(CollectionUtils.union(rewritten, other.rewritten));
	}

	@Override
	protected TypeBasedHeap wideningAux(TypeBasedHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(TypeBasedHeap other) throws SemanticException {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rewritten == null) ? 0 : rewritten.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeBasedHeap other = (TypeBasedHeap) obj;
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}
}
