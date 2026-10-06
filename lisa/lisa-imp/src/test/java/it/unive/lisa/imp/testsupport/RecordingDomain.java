package it.unive.lisa.imp.testsupport;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// a hand-rolled fake AbstractDomain: it does not compute anything, it just
// records every expression it is asked to compute the semantics of, and lets
// a test configure the runtime types it should report for specific
// expressions (by identity), so that an Expression/Statement's own
// type-checking logic can be exercised precisely
public class RecordingDomain
		implements
		AbstractDomain<UnitLattice> {

	public final List<SymbolicExpression> smallStepCalls = new ArrayList<>();

	public final List<SymbolicExpression> assignCalls = new ArrayList<>();

	private final Map<SymbolicExpression, Set<Type>> runtimeTypes = new HashMap<>();

	public void setRuntimeTypes(
			SymbolicExpression expression,
			Set<Type> types) {
		runtimeTypes.put(expression, types);
	}

	private Set<Type> runtimeTypesOf(
			SymbolicExpression expression) {
		return runtimeTypes.getOrDefault(expression, Collections.singleton(Untyped.INSTANCE));
	}

	@Override
	public UnitLattice assign(
			UnitLattice state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		assignCalls.add(expression);
		return state;
	}

	@Override
	public UnitLattice smallStepSemantics(
			UnitLattice state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		smallStepCalls.add(expression);
		return state;
	}

	@Override
	public UnitLattice assume(
			UnitLattice state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return state;
	}

	@Override
	public UnitLattice makeLattice() {
		return UnitLattice.INSTANCE;
	}

	@Override
	public UnitLattice onCallReturn(
			UnitLattice entryState,
			UnitLattice callres,
			ProgramPoint call)
			throws SemanticException {
		return entryState;
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
	}

	@Override
	public SemanticOracle makeOracle(
			UnitLattice state) {
		return new SemanticOracle() {

			@Override
			public EventQueue getEventQueue() {
				return null;
			}

			@Override
			public boolean hasWholeValueAnlysis() {
				return false;
			}

			@Override
			public Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints(
					it.unive.lisa.analysis.value.ValueDomain<?> requesting,
					it.unive.lisa.symbolic.value.ValueExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Set<Type> getRuntimeTypesOf(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				return runtimeTypesOf(e);
			}

			@Override
			public Type getDynamicTypeOf(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet rewrite(
					SymbolicExpression expression,
					ProgramPoint pp)
					throws SemanticException {
				// Analysis.assign() requires rewriting a non-Identifier
				// assignment target (e.g. a heap AccessChild) down to actual
				// Identifier(s) before it can delegate to the domain; since
				// this fake tracks no real heap structure, synthesize a
				// stand-in Identifier for it instead of throwing, so that
				// statements assigning to heap locations (array/field
				// initialization, etc.) can still be exercised
				if (expression instanceof Identifier)
					return new it.unive.lisa.lattices.ExpressionSet(expression);
				return new it.unive.lisa.lattices.ExpressionSet(
						new it.unive.lisa.symbolic.value.Variable(
								expression.getStaticType(),
								"rewritten$" + expression,
								expression.getCodeLocation()));
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet rewrite(
					it.unive.lisa.lattices.ExpressionSet expressions,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability alias(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet reachableFrom(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability isReachableFrom(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability areMutuallyReachable(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

		};
	}

}
