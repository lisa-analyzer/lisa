package it.unive.lisa.analysis.impl.heap.pointbased;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A heap domain tracking sets of {@link AllocationSite}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSites extends SetLattice<AllocationSites, AllocationSite>
		implements NonRelationalHeapDomain<AllocationSites> {

	private static final AllocationSites TOP = new AllocationSites(new HashSet<>(), true);
	private static final AllocationSites BOTTOM = new AllocationSites(new HashSet<>(), false);

	private final boolean isTop;

	/**
	 * Builds an instance of HeapIdentiferSetLattice, corresponding to the top
	 * element.
	 */
	public AllocationSites() {
		this(new HashSet<>(), true);
	}

	private AllocationSites(Set<AllocationSite> set, boolean isTop) {
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
	public AllocationSites top() {
		return TOP;
	}

	@Override
	public AllocationSites bottom() {
		return BOTTOM;
	}

	@Override
	public AllocationSites mk(Set<AllocationSite> set) {
		return new AllocationSites(set, false);
	}

	@Override
	public Iterator<AllocationSite> iterator() {
		return this.elements.iterator();
	}

	@Override
	public AllocationSites eval(SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment, ProgramPoint pp) {
		return new AllocationSites(Collections.singleton((AllocationSite) expression), false);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment, ProgramPoint pp) {
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

	@Override
	protected AllocationSites lubAux(AllocationSites other) throws SemanticException {
		Set<AllocationSite> lub = new HashSet<>();
		lub.addAll(elements.stream().filter(t -> t.isWeak()).collect(Collectors.toSet()));
		lub.addAll(other.elements.stream().filter(t -> t.isWeak()).collect(Collectors.toSet()));

		lub.addAll(elements.stream().filter(t1 -> !t1.isWeak() &&
				(!other.elements.stream().filter(t2 -> t2.getId().equals(t1.getId()) && !t2.isWeak())
						.collect(Collectors.toSet()).isEmpty()
						|| other.elements.stream().filter(t2 -> t2.getId().equals(t1.getId()))
								.collect(Collectors.toSet()).isEmpty()))
				.collect(Collectors.toSet()));

		lub.addAll(other.elements.stream().filter(t1 -> !t1.isWeak() &&
				(!elements.stream().filter(t2 -> t2.getId().equals(t1.getId()) && !t2.isWeak())
						.collect(Collectors.toSet()).isEmpty()
						|| elements.stream().filter(t2 -> t2.getId().equals(t1.getId())).collect(Collectors.toSet())
								.isEmpty()))
				.collect(Collectors.toSet()));

		return new AllocationSites(lub, false);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return id.getDynamicType().isPointerType() || id.getDynamicType().isUntyped();
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return expression.getDynamicType().isPointerType() || expression.getDynamicType().isUntyped();
	}
}
