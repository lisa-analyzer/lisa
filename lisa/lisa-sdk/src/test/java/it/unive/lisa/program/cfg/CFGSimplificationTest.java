package it.unive.lisa.program.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.Collection;
import java.util.HashSet;
import org.junit.Test;

public class CFGSimplificationTest {

	@Test
	public void testSimpleSimplification() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		ClassUnit unit = new ClassUnit(unknown, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "foo",
				false);
		CFG first = new CFG(new CodeMemberDescriptor(unknown, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new VariableRef(first, unknown, "a"));
		NoOp noop = new NoOp(first, unknown);
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CodeMemberDescriptor(unknown, unit, true, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new VariableRef(second, unknown, "a"));
		ret = new Return(second, unknown, new VariableRef(second, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testDoubleSimplification() throws ProgramValidationException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		SourceCodeLocation unknownLocation2 = new SourceCodeLocation("fake", 0, 1);
		ClassUnit unit = new ClassUnit(unknownLocation, new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"foo", false);
		CFG first = new CFG(new CodeMemberDescriptor(unknownLocation, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknownLocation, new VariableRef(first, unknownLocation, "x"),
				new VariableRef(first, unknownLocation, "a"));
		NoOp noop1 = new NoOp(first, unknownLocation);
		NoOp noop2 = new NoOp(first, unknownLocation2);
		Return ret = new Return(first, unknownLocation, new VariableRef(first, unknownLocation, "x"));
		first.addNode(assign, true);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		CFG second = new CFG(new CodeMemberDescriptor(unknownLocation, unit, true, "foo"));
		assign = new Assignment(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x"),
				new VariableRef(second, unknownLocation, "a"));
		ret = new Return(second, unknownLocation, new VariableRef(second, unknownLocation, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testConditionalSimplification() throws ProgramValidationException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		SourceCodeLocation unknownLocation2 = new SourceCodeLocation("fake", 0, 1);
		ClassUnit unit = new ClassUnit(unknownLocation, new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"foo", false);
		CFG first = new CFG(new CodeMemberDescriptor(unknownLocation, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknownLocation, new VariableRef(first, unknownLocation, "x"),
				new VariableRef(first, unknownLocation, "a"));
		VariableRef gt = new VariableRef(first, unknownLocation, "x");
		VariableRef print = new VariableRef(first, unknownLocation, "f");
		NoOp noop1 = new NoOp(first, unknownLocation);
		NoOp noop2 = new NoOp(first, unknownLocation2);
		Return ret = new Return(first, unknownLocation, new VariableRef(first, unknownLocation, "x"));
		first.addNode(assign, true);
		first.addNode(gt);
		first.addNode(print);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, gt));
		first.addEdge(new TrueEdge(gt, print));
		first.addEdge(new FalseEdge(gt, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(print, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		Collection<Statement> tbranch = new HashSet<>();
		tbranch.add(print);
		Collection<Statement> fbranch = new HashSet<>();
		tbranch.add(noop1);
		first.addControlFlowStructure(new IfThenElse(first.getNodeList(), gt, noop2, tbranch, fbranch));

		CFG second = new CFG(new CodeMemberDescriptor(unknownLocation, unit, true, "foo"));
		assign = new Assignment(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x"),
				new VariableRef(second, unknownLocation, "a"));
		gt = new VariableRef(second, unknownLocation, "x");
		print = new VariableRef(second, unknownLocation, "f");
		ret = new Return(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x"));

		second.addNode(assign, true);
		second.addNode(gt);
		second.addNode(print);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, gt));
		second.addEdge(new TrueEdge(gt, print));
		second.addEdge(new FalseEdge(gt, ret));
		second.addEdge(new SequentialEdge(print, ret));

		tbranch = new HashSet<>();
		tbranch.add(print);
		fbranch = new HashSet<>();
		second.addControlFlowStructure(new IfThenElse(second.getNodeList(), gt, ret, tbranch, fbranch));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
		ControlFlowStructure exp = second.getControlFlowStructures().iterator().next();
		ControlFlowStructure act = first.getControlFlowStructures().iterator().next();
		assertEquals("Simplification did not update control flow structures", exp, act);
	}

	@Test
	public void testSimplificationWithDuplicateStatements() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		ClassUnit unit = new ClassUnit(unknown, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "foo",
				false);
		CFG first = new CFG(new CodeMemberDescriptor(unknown, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new VariableRef(first, unknown, "a"));
		NoOp noop = new NoOp(first, unknown);
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CodeMemberDescriptor(unknown, unit, true, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new VariableRef(second, unknown, "a"));
		ret = new Return(second, unknown, new VariableRef(first, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheStart() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		ClassUnit unit = new ClassUnit(unknown, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "foo",
				false);
		CFG first = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		NoOp start = new NoOp(first, unknown);
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new VariableRef(first, unknown, "a"));
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(start, true);
		first.addNode(assign);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, ret));
		first.addEdge(new SequentialEdge(start, assign));

		CFG second = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new VariableRef(second, unknown, "a"));
		ret = new Return(second, unknown, new VariableRef(first, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheEnd() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		ClassUnit unit = new ClassUnit(unknown, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "foo",
				false);
		CFG first = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		Assignment assign1 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new VariableRef(first, unknown, "a"));
		Assignment assign2 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "y"),
				new VariableRef(first, unknown, "a"));
		NoOp end = new NoOp(first, unknown);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(end);
		first.addEdge(new SequentialEdge(assign1, assign2));
		first.addEdge(new SequentialEdge(assign2, end));

		CFG second = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		assign1 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new VariableRef(second, unknown, "a"));
		assign2 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "y"),
				new VariableRef(second, unknown, "a"));

		second.addNode(assign1, true);
		second.addNode(assign2);

		second.addEdge(new SequentialEdge(assign1, assign2));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheEndWithBranch() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		ClassUnit unit = new ClassUnit(unknown, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "foo",
				false);
		CFG first = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		Assignment assign1 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "b"),
				new VariableRef(first, unknown, "b"));
		Assignment assign2 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new VariableRef(first, unknown, "a"));
		Assignment assign3 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "y"),
				new VariableRef(first, unknown, "a"));
		NoOp end = new NoOp(first, unknown);
		first.addNode(end);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(assign3);
		first.addEdge(new TrueEdge(assign1, assign2));
		first.addEdge(new FalseEdge(assign1, assign3));
		first.addEdge(new SequentialEdge(assign2, end));
		first.addEdge(new SequentialEdge(assign3, end));

		Collection<Statement> tbranch = new HashSet<>();
		tbranch.add(assign2);
		Collection<Statement> fbranch = new HashSet<>();
		fbranch.add(assign3);
		first.addControlFlowStructure(new IfThenElse(first.getNodeList(), assign1, end, tbranch, fbranch));

		CFG second = new CFG(new CodeMemberDescriptor(unknown, unit, false, "foo"));
		assign1 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "b"),
				new VariableRef(second, unknown, "b"));
		assign2 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new VariableRef(second, unknown, "a"));
		assign3 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "y"),
				new VariableRef(first, unknown, "a"));
		second.addNode(assign1, true);
		second.addNode(assign2);
		second.addNode(assign3);
		second.addEdge(new TrueEdge(assign1, assign2));
		second.addEdge(new FalseEdge(assign1, assign3));

		tbranch = new HashSet<>();
		tbranch.add(assign2);
		fbranch = new HashSet<>();
		fbranch.add(assign3);
		second.addControlFlowStructure(new IfThenElse(second.getNodeList(), assign1, null, tbranch, fbranch));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
		ControlFlowStructure exp = second.getControlFlowStructures().iterator().next();
		ControlFlowStructure act = first.getControlFlowStructures().iterator().next();
		assertEquals("Simplification did not update control flow structures", exp, act);
	}

	@Test
	public void testIssue210() throws ProgramValidationException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		SourceCodeLocation unknownLocation2 = new SourceCodeLocation("fake", 0, 1);
		ClassUnit unit = new ClassUnit(unknownLocation, new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"foo", false);
		CFG first = new CFG(new CodeMemberDescriptor(unknownLocation, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknownLocation, new VariableRef(first, unknownLocation, "x"),
				new VariableRef(first, unknownLocation, "a"));
		VariableRef gt = new VariableRef(first, unknownLocation, "x");
		NoOp noop = new NoOp(first, unknownLocation);
		VariableRef print1 = new VariableRef(first, unknownLocation, "f");
		Return ret1 = new Return(first, unknownLocation, new VariableRef(first, unknownLocation, "x"));

		VariableRef print2 = new VariableRef(first, unknownLocation2, "f");
		Return ret2 = new Return(first, unknownLocation2, new VariableRef(first, unknownLocation2, "f"));

		first.addNode(assign, true);
		first.addNode(gt);
		first.addNode(noop);
		first.addNode(print1);
		first.addNode(ret1);
		first.addNode(print2);
		first.addNode(ret2);
		first.addEdge(new SequentialEdge(assign, gt));
		first.addEdge(new TrueEdge(gt, print1));
		first.addEdge(new SequentialEdge(print1, ret1));
		first.addEdge(new FalseEdge(gt, print2));
		first.addEdge(new SequentialEdge(print2, ret2));

		first.simplify();
		first.validate();
	}
}
