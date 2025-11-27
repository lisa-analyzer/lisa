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

public class ReachLattice
		extends
		FunctionalLattice<ReachLattice, ProgramPoint, ReachLattice.ReachabilityStatus>
		implements
		AbstractLattice<ReachLattice> {

	public ReachLattice() {
		super(ReachabilityStatus.POSSIBLY_REACHABLE);
	}

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

	public static class ReachabilityStatus
			implements
			BaseLattice<ReachabilityStatus> {

		public static final ReachabilityStatus REACHABLE = new ReachabilityStatus();
		public static final ReachabilityStatus POSSIBLY_REACHABLE = new ReachabilityStatus();
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

	public ReachLattice setToReachable() {
		if (lattice == ReachabilityStatus.REACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.REACHABLE, function);
	}

	public ReachLattice setToUnreachable() {
		if (lattice == ReachabilityStatus.UNREACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.UNREACHABLE, function);
	}

	public ReachLattice setToPossiblyReachable() {
		if (lattice == ReachabilityStatus.POSSIBLY_REACHABLE)
			return this;
		return new ReachLattice(ReachabilityStatus.POSSIBLY_REACHABLE, function);
	}
}