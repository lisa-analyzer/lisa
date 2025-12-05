package it.unive.lisa.lattices;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A lattice tracking the reachability of the current program point as a
 * {@link ReachabilityStatus}. To allow merging of reachability coming from
 * different branches, this class extends {@link FunctionalLattice} to map
 * program points to their reachability status. Elements in the mapping are
 * guards traversed during the analysis and whose body has not been fully
 * analyzed yet. Each guard is mapped to a {@link ReachabilityStatus}
 * representing the reachability of the <b>first</b> time the guard was reached.
 * This allows to properly restore the reachability after loops.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReachLattice
		extends
		FunctionalLattice<ReachLattice, ProgramPoint, ReachLattice.ReachabilityStatus>
		implements
		AbstractLattice<ReachLattice> {

	/**
	 * Builds the top reachability lattice, whose state is
	 * {@link ReachabilityStatus#POSSIBLY_REACHABLE}.
	 */
	public ReachLattice() {
		super(ReachabilityStatus.POSSIBLY_REACHABLE);
	}

	/**
	 * Builds the reachability lattice.
	 *
	 * @param lattice  the reachability status
	 * @param function the mapping from program points to their reachability
	 */
	public ReachLattice(
			ReachabilityStatus lattice,
			Map<ProgramPoint, ReachabilityStatus> function) {
		super(lattice, function);
	}

	@Override
	public StructuredRepresentation representation() {
		return lattice.representation();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public ReachLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public ReachLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public ReachLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public ReachLattice top() {
		return isTop() ? this : new ReachLattice(ReachabilityStatus.POSSIBLY_REACHABLE, null);
	}

	@Override
	public ReachLattice bottom() {
		return isBottom() ? this : new ReachLattice(ReachabilityStatus.UNREACHABLE, null);
	}

	@Override
	public ReachLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public ReachLattice popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public ReachabilityStatus stateOfUnknown(
			ProgramPoint key) {
		return ReachabilityStatus.POSSIBLY_REACHABLE;
	}

	@Override
	public ReachLattice mk(
			ReachabilityStatus lattice,
			Map<ProgramPoint, ReachabilityStatus> function) {
		return new ReachLattice(lattice, function);
	}

	/**
	 * A lattice representing the reachability status of an execution state.
	 * This lattice has three elements, organized vertically:<br>
	 * <br>
	 * 
	 * <pre>
	 * POSSIBLY_REACHABLE
	 * 		|
	 * 		REACHABLE
	 * 		|
	 * 		UNREACHABLE
	 * </pre>
	 * 
	 * <br>
	 * where {@link #REACHABLE} means that the execution state is definitely
	 * reachable, {@link #UNREACHABLE} means that the execution state is
	 * definitely unreachable, and {@link #POSSIBLY_REACHABLE} means that the
	 * execution state may or may not be reachable.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class ReachabilityStatus
			implements
			BaseLattice<ReachabilityStatus> {

		/** The reachable execution state. */
		public static final ReachabilityStatus REACHABLE = new ReachabilityStatus();
		/** The possibly reachable execution state. */
		public static final ReachabilityStatus POSSIBLY_REACHABLE = new ReachabilityStatus();
		/** The unreachable execution state. */
		public static final ReachabilityStatus UNREACHABLE = new ReachabilityStatus();

		private ReachabilityStatus() {
		}

		@Override
		public ReachabilityStatus top() {
			return POSSIBLY_REACHABLE;
		}

		@Override
		public ReachabilityStatus bottom() {
			return UNREACHABLE;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(
					this == REACHABLE
							? "REACHABLE"
							: this == POSSIBLY_REACHABLE
									? "POSSIBLY_REACHABLE"
									: "UNREACHABLE");
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public ReachabilityStatus lubAux(
				ReachabilityStatus other)
				throws SemanticException {
			// should never happen
			return POSSIBLY_REACHABLE;
		}

		@Override
		public boolean lessOrEqualAux(
				ReachabilityStatus other)
				throws SemanticException {
			// should never happen
			return false;
		}
	}

	@Override
	public ReachLattice withTopMemory() {
		return this;
	}

	@Override
	public ReachLattice withTopValues() {
		return this;
	}

	@Override
	public ReachLattice withTopTypes() {
		return this;
	}

	/**
	 * Yields a copy of this lattice, modified to have its reachability set to
	 * reachable.
	 * 
	 * @return the modified copy
	 */
	public ReachLattice setToReachable() {
		if (lattice == ReachabilityStatus.REACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.REACHABLE, function);
	}

	/**
	 * Yields a copy of this lattice, modified to have its reachability set to
	 * unreachable.
	 * 
	 * @return the modified copy
	 */
	public ReachLattice setToUnreachable() {
		if (lattice == ReachabilityStatus.UNREACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.UNREACHABLE, function);
	}

	/**
	 * Yields a copy of this lattice, modified to have its reachability set to
	 * possibly reachable.
	 * 
	 * @return the modified copy
	 */
	public ReachLattice setToPossiblyReachable() {
		if (lattice == ReachabilityStatus.POSSIBLY_REACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.POSSIBLY_REACHABLE, function);
	}
}
