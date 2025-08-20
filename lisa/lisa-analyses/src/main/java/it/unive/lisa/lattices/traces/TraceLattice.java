package it.unive.lisa.lattices.traces;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * A lattice that associates an abstract state to each execution trace. This is
 * implemented as a {@link FunctionalLattice} whose keys are the
 * {@link ExecutionTrace}s, and whose values are the abstract states associated
 * to those traces. All traces can be collapsed into a single abstract state by
 * {@link #collapse()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of the abstract states associated to traces
 */
public class TraceLattice<A extends AbstractLattice<A>>
		extends
		FunctionalLattice<TraceLattice<A>, ExecutionTrace, A>
		implements
		AbstractLattice<TraceLattice<A>> {

	/**
	 * Builds a new instance of this lattice.
	 * 
	 * @param lattice a singleton of the underlying abstract states
	 */
	public TraceLattice(
			A lattice) {
		super(lattice);
	}

	/**
	 * Builds a new instance of this lattice, with the given lattice and
	 * function.
	 * 
	 * @param lattice  a singleton of the underlying abstract states
	 * @param function a mapping from execution traces to abstract states
	 */
	public TraceLattice(
			A lattice,
			Map<ExecutionTrace, A> function) {
		super(lattice, function);
	}

	@Override
	public TraceLattice<A> mk(
			A lattice,
			Map<ExecutionTrace, A> function) {
		return new TraceLattice<>(lattice, function);
	}

	@Override
	public TraceLattice<A> top() {
		return new TraceLattice<>(lattice.top(), null);
	}

	@Override
	public TraceLattice<A> bottom() {
		return new TraceLattice<>(lattice.bottom(), null);
	}

	@Override
	public TraceLattice<A> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifier(id, pp));
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifiers(ids, pp));
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifiersIf(test, pp));
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().pushScope(token, pp));
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().popScope(token, pp));
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		if (isTop() || isBottom() || function == null)
			return false;

		for (Entry<ExecutionTrace, A> trace : this)
			if (trace.getValue().knowsIdentifier(id))
				return true;
		return false;
	}

	@Override
	public A stateOfUnknown(
			ExecutionTrace key) {
		return lattice.bottom();
	}

	@Override
	public TraceLattice<A> withTopMemory() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopMemory());
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> withTopValues() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopValues());
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public TraceLattice<A> withTopTypes() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopTypes());
		return new TraceLattice<>(lattice, result);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		if (function == null)
			return new StringRepresentation("empty");

		return new MapRepresentation(function, StringRepresentation::new, AbstractLattice::representation);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	/**
	 * Collapses all of the traces contained in this domain, returning a unique
	 * abstract state that over-approximates all of them.
	 * 
	 * @return the collapsed state
	 */
	public A collapse() {
		if (isTop())
			return lattice.top();

		A result = lattice.bottom();
		if (isBottom() || function == null)
			return result;

		try {
			for (Entry<ExecutionTrace, A> trace : this)
				result = result.lub(trace.getValue());
		} catch (SemanticException e) {
			return result.bottom();
		}
		return result;
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> lattice) {
		Collection<D> result = AbstractLattice.super.getAllLatticeInstances(lattice);
		if (isTop())
			return this.lattice.top().getAllLatticeInstances(lattice);
		else if (isBottom() || function == null)
			return this.lattice.bottom().getAllLatticeInstances(lattice);

		for (A dom : getValues())
			result.addAll(dom.getAllLatticeInstances(lattice));
		return result;
	}

}
