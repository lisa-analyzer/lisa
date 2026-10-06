package it.unive.lisa.program.cfg.statement.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AccessInstanceGlobalTest {

	private static final class TestUnitType
			implements
			UnitType {

		private final CompilationUnit unit;

		private TestUnitType(
				CompilationUnit unit) {
			this.unit = unit;
		}

		@Override
		public CompilationUnit getUnit() {
			return unit;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this || other.isUntyped();
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return other == this ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

	}

	private final Variable receiverOperand = new Variable(Untyped.INSTANCE, "receiver", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private void setup() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
	}

	private AccessInstanceGlobal accessInstanceGlobal(
			String target) {
		VariableRef receiver = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "receiver");
		return new AccessInstanceGlobal(TestFixtures.CFG, TestFixtures.LOCATION, receiver, target);
	}

	// regression test for a fixed bug: the traversal loop's dedup check used
	// to track the fixed starting unit instead of the loop's own current
	// unit, so only the very first unit in the hierarchy traversal was ever
	// actually examined; an instance global declared on any ancestor (not the
	// receiver's own most-derived unit) was silently never found. This test
	// specifically exercises the second (ancestor) step of the traversal.
	@Test
	public void findsAnInstanceGlobalDeclaredOnAnAncestorUnit()
			throws SemanticException {
		setup();
		ClassUnit superUnit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Super", false);
		ClassUnit subUnit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Sub", false);
		subUnit.addAncestor(superUnit);

		// the target field is declared ONLY on the ancestor, never on Sub
		// itself, so this can only succeed if the traversal actually reaches
		// Super
		Global field = new Global(TestFixtures.LOCATION, superUnit, "field", true, Int32Type.INSTANCE);
		superUnit.addInstanceGlobal(field);

		TestUnitType subUnitType = new TestUnitType(subUnit);
		domain.setRuntimeTypes(receiverOperand, Set.of(new ReferenceType(subUnitType)));

		AccessInstanceGlobal access = accessInstanceGlobal("field");
		AnalysisState<UnitLattice> result = access.fwdUnarySemantics(
				interprocedural, state, receiverOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		AccessChild built = (AccessChild) domain.smallStepCalls.get(0);
		HeapDereference container = (HeapDereference) built.getContainer();
		assertEquals(receiverOperand, container.getExpression());
		GlobalVariable child = (GlobalVariable) built.getChild();
		assertEquals("field", child.getName());
		assertEquals(Int32Type.INSTANCE, child.getStaticType());
		assertFalse(result.getExecutionState().isBottom());
	}

	@Test
	public void findsAnInstanceGlobalDeclaredDirectlyOnTheReceiverUnit()
			throws SemanticException {
		setup();
		ClassUnit unit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Holder", false);
		Global field = new Global(TestFixtures.LOCATION, unit, "field", true, Int32Type.INSTANCE);
		unit.addInstanceGlobal(field);

		TestUnitType unitType = new TestUnitType(unit);
		domain.setRuntimeTypes(receiverOperand, Set.of(new ReferenceType(unitType)));

		AccessInstanceGlobal access = accessInstanceGlobal("field");
		AnalysisState<UnitLattice> result = access.fwdUnarySemantics(
				interprocedural, state, receiverOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		assertFalse(result.getExecutionState().isBottom());
	}

	// when the target cannot be resolved anywhere in the hierarchy, the
	// method falls back to an untyped, best-effort access instead of bottom
	@Test
	public void unresolvedTargetFallsBackToAnUntypedAccessChild()
			throws SemanticException {
		setup();
		ClassUnit unit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Holder", false);
		// no instance global is registered on this unit at all

		TestUnitType unitType = new TestUnitType(unit);
		domain.setRuntimeTypes(receiverOperand, Set.of(new ReferenceType(unitType)));

		AccessInstanceGlobal access = accessInstanceGlobal("missing");
		AnalysisState<UnitLattice> result = access.fwdUnarySemantics(
				interprocedural, state, receiverOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		AccessChild built = (AccessChild) domain.smallStepCalls.get(0);
		GlobalVariable child = (GlobalVariable) built.getChild();
		assertEquals("missing", child.getName());
		assertEquals(Untyped.INSTANCE, child.getStaticType());
		assertFalse(result.getExecutionState().isBottom());
	}

	@Test
	public void receiverWithNoPointerTypeYieldsBottom()
			throws SemanticException {
		setup();
		domain.setRuntimeTypes(receiverOperand, Set.of(Int32Type.INSTANCE));

		AccessInstanceGlobal access = accessInstanceGlobal("field");
		AnalysisState<UnitLattice> result = access.fwdUnarySemantics(
				interprocedural, state, receiverOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
