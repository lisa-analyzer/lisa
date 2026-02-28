package it.unive.lisa.lattices;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * An abstract state that stores the history of the previous fixpoint
 * iterations, to see how the abstraction for a given instruction has evolved
 * during a fixpoint. This is concretely implemented as a recursive stack, where
 * each element is composed by a stack of previous states and the current state.
 * Iterating over instances of this class yields the inner states from oldest to
 * newest.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} embedded in this state
 */
public class HistoryState<A extends AbstractLattice<A>>
		implements
		AbstractLattice<HistoryState<A>>,
		BaseLattice<HistoryState<A>>,
		Iterable<A> {

	private final A current;

	private final HistoryState<A> previous;

	/**
	 * Builds the history state, having the given state as current and no
	 * previous states.
	 * 
	 * @param current the current state
	 */
	public HistoryState(
			A current) {
		this(current, null);
	}

	private HistoryState(
			A current,
			HistoryState<A> previous) {
		this.current = current;
		this.previous = previous;
	}

	/**
	 * Yields the current (i.e., most recent) state.
	 * 
	 * @return the current state
	 */
	public A head() {
		return current;
	}

	/**
	 * Yields a new history state where the current (i.e., most recent) state is
	 * replaced by the given one, and the previous states are preserved.
	 * 
	 * @param head the new current state
	 * 
	 * @return the new history state
	 */
	public HistoryState<A> withHead(
			A head) {
		return new HistoryState<>(head, previous);
	}

	private HistoryState<A> reverse() {
		HistoryState<A> reversed = null;
		HistoryState<A> ptr = this;
		while (ptr != null) {
			reversed = new HistoryState<>(ptr.current, reversed);
			ptr = ptr.previous;
		}
		return reversed;
	}

	@Override
	public Iterator<A> iterator() {
		return new Iterator<A>() {
			private HistoryState<A> current = HistoryState.this.reverse();

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public A next() {
				A value = current.current;
				current = current.previous;
				return value;
			}
		};
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return current.knowsIdentifier(id);
	}

	@Override
	public HistoryState<A> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(current.forgetIdentifier(id, pp));
	}

	@Override
	public HistoryState<A> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(current.forgetIdentifiersIf(test, pp));
	}

	@Override
	public HistoryState<A> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(current.forgetIdentifiers(ids, pp));
	}

	@Override
	public HistoryState<A> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(current.pushScope(token, pp));
	}

	@Override
	public HistoryState<A> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(current.popScope(token, pp));
	}

	@Override
	public HistoryState<A> top() {
		return new HistoryState<>(current.top());
	}

	@Override
	public boolean isTop() {
		return current.isTop() && previous == null;
	}

	@Override
	public HistoryState<A> bottom() {
		return new HistoryState<>(current.bottom());
	}

	@Override
	public boolean isBottom() {
		return current.isBottom() && previous == null;
	}

	@Override
	public boolean lessOrEqualAux(
			HistoryState<A> other)
			throws SemanticException {
		return current.lessOrEqual(other.current);
	}

	@Override
	public HistoryState<A> lubAux(
			HistoryState<A> other)
			throws SemanticException {
		return new HistoryState<>(current.lub(other.current));
	}

	@Override
	public HistoryState<A> upchainAux(
			HistoryState<A> other)
			throws SemanticException {
		// this represents the history of states for a given
		// instruction, while other represents the new state computed
		// for that instruction. The latter's history comes from another
		// instruction, so we discard it and just add its current state
		// to this
		return new HistoryState<>(this.current.lub(other.current), this);
	}

	@Override
	public HistoryState<A> glbAux(
			HistoryState<A> other)
			throws SemanticException {
		return new HistoryState<>(current.glb(other.current));
	}

	@Override
	public HistoryState<A> downchainAux(
			HistoryState<A> other)
			throws SemanticException {
		// this represents the history of states for a given
		// instruction, while other represents the new state computed
		// for that instruction. The latter's history comes from another
		// instruction, so we discard it and just add its current state
		// to this
		return new HistoryState<>(this.current.glb(other.current), this);
	}

	@Override
	public HistoryState<A> wideningAux(
			HistoryState<A> other)
			throws SemanticException {
		// this represents the history of states for a given
		// instruction, while other represents the new state computed
		// for that instruction. The latter's history comes from another
		// instruction, so we discard it and just add its current state
		// to this
		return new HistoryState<>(this.current.widening(other.current), this);
	}

	@Override
	public HistoryState<A> narrowingAux(
			HistoryState<A> other)
			throws SemanticException {
		// this represents the history of states for a given
		// instruction, while other represents the new state computed
		// for that instruction. The latter's history comes from another
		// instruction, so we discard it and just add its current state
		// to this
		return new HistoryState<>(this.current.narrowing(other.current), this);
	}

	@Override
	public HistoryState<A> withTopMemory() {
		return new HistoryState<>(current.withTopMemory(), previous);
	}

	@Override
	public HistoryState<A> withTopValues() {
		return new HistoryState<>(current.withTopValues(), previous);
	}

	@Override
	public HistoryState<A> withTopTypes() {
		return new HistoryState<>(current.withTopTypes(), previous);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((current == null) ? 0 : current.hashCode());
		result = prime * result + ((previous == null) ? 0 : previous.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HistoryState<?> other = (HistoryState<?>) obj;
		if (current == null) {
			if (other.current != null)
				return false;
		} else if (!current.equals(other.current))
			return false;
		if (previous == null) {
			if (other.previous != null)
				return false;
		} else if (!previous.equals(other.previous))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		List<StructuredRepresentation> elements = new LinkedList<>();
		HistoryState<A> ptr = this;
		while (ptr != null) {
			elements.add(ptr.current.representation());
			ptr = ptr.previous;
		}

		return new ListRepresentation(elements);
	}

}
