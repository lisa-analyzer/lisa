package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A heap domain tracking sets of {@link HeapIdentifier}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class HeapIdentifierSetLattice extends SetLattice<HeapIdentifierSetLattice, AllocationSiteHeapIdentifier>
		implements Iterable<AllocationSiteHeapIdentifier>, NonRelationalHeapDomain<HeapIdentifierSetLattice> {

	private static final HeapIdentifierSetLattice TOP = new HeapIdentifierSetLattice(new HashSet<>(), true);
	private static final HeapIdentifierSetLattice BOTTOM = new HeapIdentifierSetLattice(new HashSet<>(), false);

	private final boolean isTop;

	/**
	 * Builds an instance of HeapIdentiferSetLattice, corresponding to the top
	 * element.
	 */
	public HeapIdentifierSetLattice() {
		this(new HashSet<>(), true);
	}

	private HeapIdentifierSetLattice(Set<AllocationSiteHeapIdentifier> set, boolean isTop) {
		super(set);
		this.isTop = isTop;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public boolean isBottom() {
		return !isTop && elements.isEmpty();
	}

	@Override
	public HeapIdentifierSetLattice top() {
		return TOP;
	}

	@Override
	public HeapIdentifierSetLattice bottom() {
		return BOTTOM;
	}

	@Override
	public HeapIdentifierSetLattice mk(Set<AllocationSiteHeapIdentifier> set) {
		return new HeapIdentifierSetLattice(set, false);
	}

	@Override
	public Iterator<AllocationSiteHeapIdentifier> iterator() {
		return this.elements.iterator();
	}

	@Override
	public HeapIdentifierSetLattice eval(SymbolicExpression expression,
			HeapEnvironment<HeapIdentifierSetLattice> environment, ProgramPoint pp) {
		return new HeapIdentifierSetLattice(Collections.singleton((AllocationSiteHeapIdentifier) expression), false);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression,
			HeapEnvironment<HeapIdentifierSetLattice> environment, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		return super.toString();
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return Collections.emptySet();
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}
}
