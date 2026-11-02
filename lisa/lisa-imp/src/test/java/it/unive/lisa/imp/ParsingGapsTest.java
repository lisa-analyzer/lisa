package it.unive.lisa.imp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;
import it.unive.lisa.program.annotations.values.BoolAnnotationValue;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.TypeLiteral;
import it.unive.lisa.program.cfg.statement.types.Cast;
import it.unive.lisa.program.cfg.statement.types.IsInstance;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// gap-filling tests for it.unive.lisa.imp.IMPFrontend/IMPAnnotationVisitor: existing
// coverage (IMPFrontendTest, HierarchyComputationTest) only ever uses processFile(...)
// against golden fixtures, never processText(...), never asserts on annotation content
// directly, never checks the onlyMain flag, and never exercises a syntax error
public class ParsingGapsTest {

	@Test
	public void processTextParsesAValidProgram()
			throws ParsingException {
		Program p = IMPFrontend.processText("class first { foo() { return 1; } }");
		Unit first = p.getUnits().stream().filter(u -> u.getName().equals("first")).findFirst().orElse(null);
		assertNotNull(first, "'first' unit not found");
	}

	@Test
	public void onlyMainRestrictsEntryPointsToTheMainMethod()
			throws ParsingException {
		String text = "class c { main() { } other() { } }";

		Program withOnlyMain = IMPFrontend.processText(text, true);
		assertEquals(1, withOnlyMain.getEntryPoints().size());
		assertEquals("main", withOnlyMain.getEntryPoints().iterator().next().getDescriptor().getName());

		Program withAllEntries = IMPFrontend.processText(text, false);
		assertEquals(2, withAllEntries.getEntryPoints().size());
	}

	// class-level annotation attachment is broken (see
	// classLevelAnnotationsAreSilentlyDiscarded below), so this test uses a
	// FIELD annotation instead, which IMPFrontend#visitFieldDeclaration does
	// correctly wire up, to verify member/value parsing (int/bool/array) in
	// isolation from that separate bug
	@Test
	public void annotationMembersAreParsedWithTheirValues()
			throws ParsingException {
		Program p = IMPFrontend
				.processText("class c { [ann1, ann2(i = 1, j = true, k = [1, 2, 3])] field; }");
		ClassUnit c = (ClassUnit) p.getUnits().stream().filter(u -> u.getName().equals("c")).findFirst()
				.orElse(null);
		assertNotNull(c, "'c' unit not found");
		Global field = c.getInstanceGlobals(false).stream().filter(g -> g.getName().equals("field")).findFirst()
				.orElse(null);
		assertNotNull(field, "'field' global not found");

		Annotation ann1 = null;
		Annotation ann2 = null;
		for (Annotation a : field.getAnnotations())
			if (a.getAnnotationName().equals("ann1"))
				ann1 = a;
			else if (a.getAnnotationName().equals("ann2"))
				ann2 = a;
		assertNotNull(ann1, "ann1 not found");
		assertNotNull(ann2, "ann2 not found");
		assertTrue(ann1.getAnnotationMembers().isEmpty());

		assertEquals(3, ann2.getAnnotationMembers().size());
		AnnotationMember i = ann2.getAnnotationMembers().stream().filter(m -> m.getId().equals("i")).findFirst()
				.orElse(null);
		AnnotationMember j = ann2.getAnnotationMembers().stream().filter(m -> m.getId().equals("j")).findFirst()
				.orElse(null);
		AnnotationMember k = ann2.getAnnotationMembers().stream().filter(m -> m.getId().equals("k")).findFirst()
				.orElse(null);
		assertNotNull(i);
		assertNotNull(j);
		assertNotNull(k);

		assertEquals(1, ((IntAnnotationValue) i.getValue()).getInteger());
		assertTrue(((BoolAnnotationValue) j.getValue()).getBoolean());

		BasicAnnotationValue[] arr = ((ArrayAnnotationValue) k.getValue()).getArray();
		assertEquals(3, arr.length);
		assertEquals(1, ((IntAnnotationValue) arr[0]).getInteger());
		assertEquals(2, ((IntAnnotationValue) arr[1]).getInteger());
		assertEquals(3, ((IntAnnotationValue) arr[2]).getInteger());
	}

	// class (and interface) declarations support a leading "[ann1, ...]"
	// annotation block - this is exactly the syntax basic/example.imp uses
	// ("[ann1, inheritedann2, ann3(...)] class third extends second {}") -
	// and IMPFrontend#visitClassUnit/visitInterfaceUnit must attach the
	// parsed Annotations to the resulting ClassUnit/InterfaceUnit, the same
	// way visitFieldDeclaration/visitConstantDeclaration already do for
	// fields and constants.
	@Test
	public void classLevelAnnotationsAreAttachedToTheUnit()
			throws ParsingException {
		Program p = IMPFrontend.processText("[ann1] class c { }");
		Unit c = p.getUnits().stream().filter(u -> u.getName().equals("c")).findFirst().orElse(null);
		assertNotNull(c, "'c' unit not found");

		Annotations annotations = ((CompilationUnit) c).getAnnotations();
		assertFalse(annotations.isEmpty(), "expected 'ann1' to be attached to the class unit");
		assertTrue(
				annotations.getAnnotations().stream().anyMatch(a -> a.getAnnotationName().equals("ann1")),
				"expected 'ann1' among the class unit's annotations, but found " + annotations);
	}

	@Test
	public void processTextReportsASyntaxErrorAsAParsingException() {
		// missing closing brace
		ParsingException ex = assertThrows(ParsingException.class,
				() -> IMPFrontend.processText("class c { foo() { return 1; }"));
		assertNotNull(ex.getMessage());
		assertFalse(ex.getMessage().isEmpty());
	}

	@Test
	public void unresolvableEntryPointStillYieldsAllCfgsWhenOnlyMainIsFalse()
			throws ParsingException {
		// a program without any method named "main" is still valid when
		// onlyMain
		// is false: every CFG is an entry point regardless of its name
		Program p = IMPFrontend.processText("class c { foo() { } bar() { } }", false);
		CFG foo = p.getAllCFGs().stream().filter(cfg -> cfg.getDescriptor().getName().equals("foo")).findFirst()
				.orElse(null);
		CFG bar = p.getAllCFGs().stream().filter(cfg -> cfg.getDescriptor().getName().equals("bar")).findFirst()
				.orElse(null);
		assertNotNull(foo);
		assertNotNull(bar);
		assertTrue(p.getEntryPoints().contains(foo));
		assertTrue(p.getEntryPoints().contains(bar));
	}

	@Test
	public void typeTestsAndCastsAcceptPrimitiveTypes()
			throws ParsingException {
		Program p = IMPFrontend.processText("class c { m(x) { "
				+ "def a = x is int; def b = x is float; def c = x is boolean; def d = x is string; "
				+ "def e = x as int; def f = x is c; } }");
		CFG m = p.getAllCFGs().stream().filter(cfg -> cfg.getDescriptor().getName().equals("m")).findFirst().get();
		List<Type> tested = new ArrayList<>();
		List<Type> casted = new ArrayList<>();
		for (Statement st : m.getNodes()) {
			if (!(st instanceof Assignment))
				continue;
			Expression rhs = ((Assignment) st).getRight();
			if (rhs instanceof IsInstance)
				tested.add((Type) ((TypeLiteral) ((IsInstance) rhs).getRight()).getValue());
			else if (rhs instanceof Cast)
				casted.add((Type) ((TypeLiteral) ((Cast) rhs).getRight()).getValue());
		}
		assertTrue(tested.contains(Int32Type.INSTANCE), tested.toString());
		assertTrue(tested.contains(Float32Type.INSTANCE), tested.toString());
		assertTrue(tested.contains(BoolType.INSTANCE), tested.toString());
		assertTrue(tested.contains(StringType.INSTANCE), tested.toString());
		assertTrue(tested.stream().anyMatch(t -> t.toString().equals("c")), tested.toString());
		assertEquals(List.of(Int32Type.INSTANCE), casted);
	}

	@Test
	public void typeTestsBindTighterThanLogicalOperators()
			throws ParsingException {
		Program p = IMPFrontend.processText("class c { m(x, y) { def a = x is int && y is int; } }");
		CFG m = p.getAllCFGs().stream().filter(cfg -> cfg.getDescriptor().getName().equals("m")).findFirst().get();
		Expression rhs = m.getNodes().stream()
				.filter(st -> st instanceof Assignment)
				.map(st -> ((Assignment) st).getRight())
				.findFirst().get();
		// the conjunction of two type tests, not a type test on a conjunction
		assertFalse(rhs instanceof IsInstance, rhs.toString());
		assertTrue(rhs.toString().startsWith("&&("), rhs.toString());
	}
}
