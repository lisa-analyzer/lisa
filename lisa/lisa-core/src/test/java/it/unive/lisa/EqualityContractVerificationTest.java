package it.unive.lisa;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.LiSAFactory.ConfigurableComponent;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonInterference.NonInterference;
import it.unive.lisa.analysis.nonrelational.NonRelationalElement;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.ContextInsensitiveToken;
import it.unive.lisa.interprocedural.ContextSensitivityToken;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.callgraph.CallGraphEdge;
import it.unive.lisa.interprocedural.callgraph.CallGraphNode;
import it.unive.lisa.outputs.DotCFG;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.JsonReport.JsonWarning;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.call.resolution.StaticTypesResolution;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.IterableArray;
import it.unive.lisa.util.collections.externalSet.BitExternalSet;
import it.unive.lisa.util.collections.externalSet.ExternalSetCache;
import it.unive.lisa.util.collections.externalSet.UniversalExternalSet;
import it.unive.lisa.util.collections.workset.ConcurrentFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.ConcurrentLIFOWorkingSet;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix.NodeEdges;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.SingleTypeEqualsVerifierApi;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.implementations.SingleGraph;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class EqualityContractVerificationTest {

	private static final SourceCodeLocation loc = new SourceCodeLocation("fake", 0, 0);
	private static final CompilationUnit unit1 = new CompilationUnit(loc, "fake1", false);
	private static final CompilationUnit unit2 = new CompilationUnit(loc, "fake2", false);
	private static final CFGDescriptor descr1 = new CFGDescriptor(loc, unit1, false, "fake1");
	private static final CFGDescriptor descr2 = new CFGDescriptor(loc, unit2, false, "fake2");
	private static final CFG cfg1 = new CFG(descr1);
	private static final CFG cfg2 = new CFG(descr2);
	private static final AdjacencyMatrix<Statement, Edge, CFG> adj1 = new AdjacencyMatrix<>();
	private static final AdjacencyMatrix<Statement, Edge, CFG> adj2 = new AdjacencyMatrix<>();
	private static final DomainRepresentation dr1 = new StringRepresentation("foo");
	private static final DomainRepresentation dr2 = new StringRepresentation("bar");
	private static final SingleGraph g1 = new SingleGraph("a");
	private static final SingleGraph g2 = new SingleGraph("b");
	private static final UnresolvedCall uc1 = new UnresolvedCall(cfg1, loc, StaticTypesResolution.INSTANCE, false,
			"foo", "foo");
	private static final UnresolvedCall uc2 = new UnresolvedCall(cfg2, loc, StaticTypesResolution.INSTANCE, false,
			"bar", "bar");

	private static final Collection<Class<?>> tested = new HashSet<>();

	@BeforeClass
	public static void setup() {
		adj1.addNode(new Ret(cfg1, loc));
		g1.addNode("a");
	}

	private static Reflections mkReflections() {
		return new Reflections(LiSA.class, IMPFrontend.class, AnalysisException.class, new SubTypesScanner(false));
	}

	@AfterClass
	public static void ensureAllTested() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		Reflections scanner = mkReflections();
		Set<String> all = scanner.getAllTypes();
		Class<?> clazz;
		Collection<Class<?>> notTested = new HashSet<>();
		for (String cls : all) {
			clazz = Class.forName(cls);
			if (!clazz.isAnonymousClass()
					&& !clazz.isEnum()
					&& !Modifier.isAbstract(clazz.getModifiers())
					&& !Modifier.isInterface(clazz.getModifiers())
					&& !tested.contains(clazz)
					&& definesEqualsHashcode(clazz))
				notTested.add(clazz);
		}

		if (!notTested.isEmpty())
			System.err.println("The following equals/hashcode implementations have not been tested: " + notTested);

		assertTrue("Not all equals/hashcode have been tested", notTested.isEmpty());
	}

	private static boolean definesEqualsHashcode(Class<?> clazz) throws NoSuchMethodException, SecurityException {
		Class<?> equals = clazz.getMethod("equals", Object.class).getDeclaringClass();
		Class<?> hashcode = clazz.getMethod("hashCode").getDeclaringClass();
		// we want to test our implementations, not the one coming from
		// libraries
		return equals.getName().startsWith("it.unive.lisa") || hashcode.getName().startsWith("it.unive.lisa");
	}

	private static <T> void verify(Class<T> clazz, Warning... suppressions) {
		verify(clazz, true, null, suppressions);
	}

	private static <T> void verify(Class<T> clazz, Consumer<SingleTypeEqualsVerifierApi<T>> extra,
			Warning... suppressions) {
		verify(clazz, true, extra, suppressions);
	}

	private static <T> void verify(Class<T> clazz, boolean getClass, Warning... suppressions) {
		verify(clazz, getClass, null, suppressions);
	}

	private static <T> void verify(Class<T> clazz, boolean getClass, Consumer<SingleTypeEqualsVerifierApi<T>> extra,
			Warning... suppressions) {
		if (clazz.isAnonymousClass() || Modifier.isAbstract(clazz.getModifiers())
				|| Modifier.isInterface(clazz.getModifiers()))
			return;

		SingleTypeEqualsVerifierApi<T> verifier = EqualsVerifier.forClass(clazz)
				.suppress(suppressions)
				.withPrefabValues(CFG.class, cfg1, cfg2)
				.withPrefabValues(CFGDescriptor.class, descr1, descr2)
				.withPrefabValues(CompilationUnit.class, unit1, unit2)
				.withPrefabValues(AdjacencyMatrix.class, adj1, adj2)
				.withPrefabValues(DomainRepresentation.class, dr1, dr2)
				.withPrefabValues(Pair.class, Pair.of(1, 2), Pair.of(3, 4))
				.withPrefabValues(NonInterference.class, new NonInterference().top(), new NonInterference().bottom())
				.withPrefabValues(UnresolvedCall.class, uc1, uc2)
				.withPrefabValues(org.graphstream.graph.Graph.class, g1, g2);

		if (getClass)
			verifier = verifier.usingGetClass();

		if (extra != null)
			extra.accept(verifier);

		tested.add(clazz);
		verifier.verify();
	}

	@Test
	public void testConfiguration() {
		verify(LiSAConfiguration.class, Warning.NONFINAL_FIELDS);
		verify(ConfigurableComponent.class);
	}

	@Test
	public void testCollections() {
		// some of the classes here will need to suppress warnings about
		// mutability, since collections are mutable by nature

		verify(IterableArray.class);

		// caches are unique: we are fine in using object's equality and not
		// caring about fields
		verify(ExternalSetCache.class, Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED);
		// suppress nullity: the cache will never be null..
		verify(BitExternalSet.class, Warning.NULL_FIELDS, Warning.NONFINAL_FIELDS);
		verify(UniversalExternalSet.class);

		verify(AdjacencyMatrix.class, verifier -> verifier.withIgnoredFields("nextOffset"));
		verify(NodeEdges.class);

		verify(ConcurrentFIFOWorkingSet.class);
		verify(ConcurrentLIFOWorkingSet.class);
		verify(FIFOWorkingSet.class);
		verify(LIFOWorkingSet.class);
		verify(VisitOnceWorkingSet.class);
	}

	@Test
	public void testIntervalStructure() {
		verify(IntInterval.class);
		verify(MathNumber.class);
	}

	@Test
	public void testTypes() {
		Reflections scanner = mkReflections();
		for (Class<? extends Type> type : scanner.getSubTypesOf(Type.class))
			// type token is the only one with an eclipse-like equals
			verify(type, type == TypeTokenType.class);
	}

	@Test
	public void testSymbolicExpressions() {
		Reflections scanner = mkReflections();
		for (Class<? extends SymbolicExpression> expr : scanner.getSubTypesOf(SymbolicExpression.class))
			if (HeapLocation.class.isAssignableFrom(expr))
				// heap locations use only their name and weakness for equality
				verify(expr, verifier -> verifier.withOnlyTheseFields("name", "weak"));
			else if (Identifier.class.isAssignableFrom(expr))
				// identifiers use only their name for equality
				verify(expr, verifier -> verifier.withOnlyTheseFields("name"));
			else
				// location is excluded on purpose: it only brings syntactic
				// information
				verify(expr, verifier -> verifier.withIgnoredFields("location"));
	}

	@Test
	public void testStatements() {
		// suppress nullity: the verifier will try to pass in a code location
		// with null fields (not possible) and this would cause warnings

		List<String> statementFields = List.of("cfg", "offset");
		List<String> expressionFields = ListUtils.union(statementFields,
				List.of("runtimeTypes", "parent", "metaVariables"));

		Reflections scanner = mkReflections();
		for (Class<? extends Statement> st : scanner.getSubTypesOf(Statement.class))
			// the ignored fields are either mutable or auxiliary
			if (Expression.class.isAssignableFrom(st)) {
				List<String> extra = new LinkedList<>();
				if (PluggableStatement.class.isAssignableFrom(st)
						// string statements are already pluggable-friendly
						|| st.getPackageName().equals("it.unive.lisa.program.cfg.statement.string"))
					extra.add("originating");
				if (Call.class.isAssignableFrom(st))
					extra.add("source");
				if (NaryExpression.class.isAssignableFrom(st))
					extra.add("order");
				verify(st,
						verifier -> verifier
								.withIgnoredFields(ListUtils.union(expressionFields, extra).toArray(String[]::new)),
						Warning.NULL_FIELDS);
			} else
				verify(st, verifier -> verifier.withIgnoredFields(statementFields.toArray(String[]::new)),
						Warning.NULL_FIELDS);
	}

	@Test
	public void testEdges() {
		Reflections scanner = mkReflections();
		for (Class<? extends Edge> edge : scanner.getSubTypesOf(Edge.class))
			verify(edge);
	}

	@Test
	public void testAnnotations() {
		verify(Annotation.class);
		verify(Annotations.class);
		verify(AnnotationMember.class);

		Reflections scanner = mkReflections();
		for (Class<? extends AnnotationValue> value : scanner.getSubTypesOf(AnnotationValue.class))
			verify(value);
		for (Class<? extends AnnotationMatcher> matcher : scanner.getSubTypesOf(AnnotationMatcher.class))
			verify(matcher);
	}

	@Test
	public void testRepresentations() {
		Reflections scanner = mkReflections();
		for (Class<? extends DomainRepresentation> repr : scanner.getSubTypesOf(DomainRepresentation.class))
			verify(repr);
	}

	@Test
	public void testDomainsAndLattices() {
		Reflections scanner = mkReflections();
		Collection<Class<?>> testable = new HashSet<>();
		testable.addAll(scanner.getSubTypesOf(Lattice.class));
		testable.addAll(scanner.getSubTypesOf(SemanticDomain.class));
		testable.addAll(scanner.getSubTypesOf(NonRelationalElement.class));
		testable.addAll(scanner.getSubTypesOf(DataflowElement.class));

		for (Class<?> subject : testable)
			if (FunctionalLattice.class.isAssignableFrom(subject)
					|| SetLattice.class.isAssignableFrom(subject)
					|| InverseSetLattice.class.isAssignableFrom(subject))
				// fields function and elements can be null
				verify(subject, Warning.NONFINAL_FIELDS);
			else if (subject != CFGWithAnalysisResults.class)
				// we test the cfg separately
				verify(subject);
	}

	@Test
	public void testAnalysisObjects() {
		verify(HeapReplacement.class);
		verify(ScopeToken.class);
		// we consider only fields that compose the results
		// id is mutable
		verify(CFGWithAnalysisResults.class, verifier -> verifier.withOnlyTheseFields("id", "results", "entryStates"),
				Warning.NONFINAL_FIELDS);
	}

	@Test
	public void testWarnings() {
		// serialization requires non final fields
		verify(JsonWarning.class, Warning.NONFINAL_FIELDS);
		verify(it.unive.lisa.checks.warnings.Warning.class);
		Reflections scanner = mkReflections();
		for (Class<? extends it.unive.lisa.checks.warnings.Warning> warning : scanner
				.getSubTypesOf(it.unive.lisa.checks.warnings.Warning.class))
			verify(warning);
	}

	@Test
	public void testInterproceduralObjects() {
		verify(CallGraphEdge.class);
		verify(CallGraphNode.class, verifier -> verifier.withIgnoredFields("graph"));
		verify(CFGResults.class, Warning.NONFINAL_FIELDS);
		verify(FixpointResults.class, Warning.NONFINAL_FIELDS);
		Reflections scanner = mkReflections();
		for (Class<? extends ContextSensitivityToken> token : scanner.getSubTypesOf(ContextSensitivityToken.class))
			if (token == ContextInsensitiveToken.class)
				verify(token, Warning.INHERITED_DIRECTLY_FROM_OBJECT);
			else
				verify(token);
	}

	@Test
	public void testProgramStructure() {
		verify(Global.class);
		verify(Parameter.class);
		// 'overridable' is mutable
		verify(CFGDescriptor.class, Warning.NONFINAL_FIELDS);
		// scope bounds are mutable
		verify(VariableTableEntry.class, Warning.NONFINAL_FIELDS);
		Reflections scanner = mkReflections();
		for (Class<? extends CodeLocation> loc : scanner.getSubTypesOf(CodeLocation.class))
			if (loc == SyntheticLocation.class)
				// singleton instance
				verify(loc, Warning.INHERITED_DIRECTLY_FROM_OBJECT);
			else
				verify(loc);
		for (Class<? extends ControlFlowStructure> struct : scanner.getSubTypesOf(ControlFlowStructure.class))
			// first follower is mutable
			verify(struct, Warning.NONFINAL_FIELDS);
		for (Class<? extends Unit> unit : scanner.getSubTypesOf(Unit.class))
			verify(unit, Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED);
		for (Class<? extends CodeMember> cm : scanner.getSubTypesOf(CodeMember.class))
			if (!CFGWithAnalysisResults.class.isAssignableFrom(cm))
				verify(cm, Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED);
	}

	@Test
	public void testOutputs() {
		verify(JsonReport.class);
		// the fields are ignored since they are only used to build up the
		// underlying graph, and equality testing the graph will take them into
		// account
		verify(DotCFG.class, verifier -> verifier.withIgnoredFields("legend", "title", "codes", "nextCode"));
	}
}
