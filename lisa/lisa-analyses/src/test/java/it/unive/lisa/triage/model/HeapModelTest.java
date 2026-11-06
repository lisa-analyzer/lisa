package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Untyped;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests of the heap model of alarm models: reads of fields and array cells
 * after writes, new references that do not alias existing ones, and immutable
 * fields. The verifier does not check warnings against the results of the
 * analysis first, so that the model is always used.
 */
public class HeapModelTest {

	private static final String PROGRAM = "class A {\n"
			+ "  f;\n"
			+ "  ~A() {}\n"
			+ "}\n"
			+ "class test {\n"
			+ "  written() {\n"
			+ "    def o = new A();\n"
			+ "    o.f = 3;\n"
			+ "    def w = 0;\n"
			+ "    if (o.f != 3)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  input(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def o = new A();\n"
			+ "      o.f = x;\n"
			+ "      if (o.f == 42)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  aliasing() {\n"
			+ "    def a = new A();\n"
			+ "    def b = new A();\n"
			+ "    a.f = 1;\n"
			+ "    b.f = 2;\n"
			+ "    def w = 0;\n"
			+ "    if (a.f == 2)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  cells() {\n"
			+ "    def arr = new int[3];\n"
			+ "    arr[0] = 5;\n"
			+ "    arr[1] = 7;\n"
			+ "    def w = 0;\n"
			+ "    if (arr[0] != 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  cell(i) {\n"
			+ "    def w = 0;\n"
			+ "    if (i is int) {\n"
			+ "      def arr = new int[3];\n"
			+ "      arr[0] = 5;\n"
			+ "      arr[1] = 7;\n"
			+ "      if (i >= 0 && i < 3)\n"
			+ "        if (arr[i] == 7)\n"
			+ "          w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  unwritten() {\n"
			+ "    def arr = new int[3];\n"
			+ "    arr[0] = 5;\n"
			+ "    def w = 0;\n"
			+ "    if (arr[2] == 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  parameter(o) {\n"
			+ "    def w = 0;\n"
			+ "    if (o.f == 7)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  length() {\n"
			+ "    def arr = new int[3];\n"
			+ "    def w = 0;\n"
			+ "    if (arraylen(arr) != 3)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "}";

	private static Program program;

	private static SemanticTool<?, ?> tool;

	@SuppressWarnings("rawtypes")
	private static IndividualWarningVerifier verifier;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			HeapModelTest.tool = tool;
		}
	}

	@BeforeAll
	public static void analyze() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		program = IMPFrontend.processText(PROGRAM);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("heap").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
		verifier = new PdrVerifier<>().withFixpointCheck(false);
		verifier.init(tool);
	}

	private static CFG cfg(
			String name) {
		return program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
	}

	@SuppressWarnings("unchecked")
	private static VerificationResult verify(
			String name) {
		CFG cfg = cfg(name);
		Statement st = cfg.getNodes().stream().filter(n -> n.toString().equals("w = 1")).findFirst()
				.orElseThrow(() -> new AssertionError("No alarm in " + name));
		try {
			return verifier.verify(tool, new StatementMessage(st, "alarm", null, Violation.reach()));
		} catch (SemanticException e) {
			throw new AssertionError(e);
		}
	}

	private static String valueOf(
			VerificationResult result,
			String source) {
		for (ReproducingInput input : result.getReproducingInputs())
			if (input.getSource().startsWith(source))
				return input.getValue();
		throw new AssertionError("No input from " + source + " in " + result.getReproducingInputs());
	}

	@Test
	public void writtenFieldsAreRead() {
		VerificationResult result = verify("written");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void inputsFlowThroughFields() {
		VerificationResult result = verify("input");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("42", valueOf(result, "parameter x of"));
	}

	@Test
	public void newObjectsDoNotAlias() {
		VerificationResult result = verify("aliasing");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void writtenCellsAreRead() {
		VerificationResult result = verify("cells");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void cellsAreReadAtComputedIndexes() {
		VerificationResult result = verify("cell");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("1", valueOf(result, "parameter i of"));
	}

	@Test
	public void unwrittenCellsAreNotPrecise() {
		// the cell has never been written: its value is unknown, and no
		// counterexample relying on it is confirmed
		VerificationResult result = verify("unwritten");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void fieldsOfExistingObjectsAreNotPrecise() {
		// the object exists before the method starts: its fields are not
		// written by the model
		VerificationResult result = verify("parameter");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void lengthsAreFields() {
		VerificationResult result = verify("length");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void finalFieldsAreImmutable() {
		ClassUnit a = (ClassUnit) program.getUnit("A");
		ClassUnit test = (ClassUnit) program.getUnit("test");
		a.addInstanceGlobal(new Global(SyntheticLocation.INSTANCE, a, "k", true, Untyped.INSTANCE,
				new Annotations(), true));
		HeapModel heap = new HeapModel(program);
		assertTrue(heap.isImmutable(HeapModel.field("k")));
		assertFalse(heap.isImmutable(HeapModel.field("f")));
		// array lengths are immutable too
		assertTrue(heap.isImmutable(HeapModel.field("len")));
		// code of other classes cannot write k, while the code of A can
		Set<?> byTest = heap.preservedBy(test.getInstanceCodeMembers(false));
		assertTrue(byTest.contains(HeapModel.field("k")), byTest.toString());
		Set<?> byA = heap.preservedBy(a.getInstanceCodeMembers(false));
		assertFalse(byA.contains(HeapModel.field("k")), byA.toString());
		assertTrue(byA.contains(HeapModel.field("len")), byA.toString());
	}

	@Test
	public void fieldsAreQualifiedByTheirClasses() throws Exception {
		Program p = IMPFrontend.processText("class QA { f; }\n"
				+ "class QB extends QA { g; m() { def x = 0; } }\n"
				+ "class QC { f; m() { def x = 0; } }");
		ClassUnit qa = (ClassUnit) p.getUnit("QA");
		ClassUnit qb = (ClassUnit) p.getUnit("QB");
		ClassUnit qc = (ClassUnit) p.getUnit("QC");
		// h is final in QA, but not in QC
		qa.addInstanceGlobal(new Global(SyntheticLocation.INSTANCE, qa, "h", true, Untyped.INSTANCE,
				new Annotations(), true));
		qc.addInstanceGlobal(new Global(SyntheticLocation.INSTANCE, qc, "h", true, Untyped.INSTANCE));
		HeapModel heap = new HeapModel(p);

		// fields are resolved statically, from the class of the static type
		// through its ancestors
		assertEquals("QA::f", heap.qualify(ClassType.lookup("QB"), "f"));
		assertEquals("QA::f", heap.qualify(new ReferenceType(ClassType.lookup("QB")), "f"));
		assertEquals("QB::g", heap.qualify(ClassType.lookup("QB"), "g"));
		assertEquals("QC::f", heap.qualify(ClassType.lookup("QC"), "f"));
		// unknown static types and fields are not qualified
		assertEquals(null, heap.qualify(Untyped.INSTANCE, "f"));
		assertEquals(null, heap.qualify(ClassType.lookup("QB"), "unknown"));

		// a qualified field is immutable if it is final, while a field name is
		// immutable if all the fields with that name are final
		assertTrue(heap.isImmutable(HeapModel.field("QA::h")));
		assertFalse(heap.isImmutable(HeapModel.field("QC::h")));
		assertFalse(heap.isImmutable(HeapModel.field("h")));
		// only the code of QA and of its subclasses can write QA::h
		Set<?> byQC = heap.preservedBy(qc.getInstanceCodeMembers(false));
		assertTrue(byQC.contains(HeapModel.field("QA::h")), byQC.toString());
		assertFalse(byQC.contains(HeapModel.field("QC::h")), byQC.toString());
		Set<?> byQB = heap.preservedBy(qb.getInstanceCodeMembers(false));
		assertFalse(byQB.contains(HeapModel.field("QA::h")), byQB.toString());
		assertTrue(heap.immutableContents().contains(HeapModel.field("QA::h")));

		// the same field accessed in the other way
		assertEquals(Set.of(HeapModel.field("f"), HeapModel.written(HeapModel.field("f"))),
				heap.aliasesOf("QA::f", ClassType.lookup("QB")));
		assertEquals(Set.of(HeapModel.field("QA::f"), HeapModel.written(HeapModel.field("QA::f")),
				HeapModel.field("QC::f"), HeapModel.written(HeapModel.field("QC::f"))),
				heap.aliasesOf("f", Untyped.INSTANCE));
		// objects of QC cannot have the fields of QA, that is not related to
		// QC, while objects of QB have the ones of QA
		assertEquals(Set.of(HeapModel.field("QC::f"), HeapModel.written(HeapModel.field("QC::f"))),
				heap.aliasesOf("f", ClassType.lookup("QC")));
		assertEquals(Set.of(HeapModel.field("QA::f"), HeapModel.written(HeapModel.field("QA::f"))),
				heap.aliasesOf("f", new ReferenceType(ClassType.lookup("QB"))));
	}
}
