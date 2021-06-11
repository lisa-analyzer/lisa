package it.unive.lisa;

import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.implementations.SingleGraph;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import it.unive.lisa.LiSAFactory.ConfigurableComponent;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.impl.nonInterference.NonInterference;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.NonRelationalElement;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue.InferredPair;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.checks.ChecksExecutor;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphEdge;
import it.unive.lisa.interprocedural.callgraph.CallGraphNode;
import it.unive.lisa.interprocedural.impl.CFGResults;
import it.unive.lisa.interprocedural.impl.ContextInsensitiveToken;
import it.unive.lisa.interprocedural.impl.ContextSensitivityToken;
import it.unive.lisa.interprocedural.impl.FixpointResults;
import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.outputs.JsonReport.JsonWarning;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.outputs.compare.JsonReportComparer.DiffReporter;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
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
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import it.unive.lisa.util.collections.IterableArray;
import it.unive.lisa.util.collections.externalSet.BitExternalSet;
import it.unive.lisa.util.collections.externalSet.ExternalSetCache;
import it.unive.lisa.util.collections.externalSet.UniversalExternalSet;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.algorithms.Dominators;
import it.unive.lisa.util.file.FileManager;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.workset.ConcurrentFIFOWorkingSet;
import it.unive.lisa.util.workset.ConcurrentLIFOWorkingSet;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.LIFOWorkingSet;
import it.unive.lisa.util.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.SingleTypeEqualsVerifierApi;

public class EqualityContractVerification {

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

	@BeforeClass
	public static void setup() {
		adj1.addNode(new Ret(cfg1, loc));
		g1.addNode("a");
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
				.withPrefabValues(org.graphstream.graph.Graph.class, g1, g2);

		if (getClass)
			verifier = verifier.usingGetClass();

		if (extra != null)
			extra.accept(verifier);

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

		verify(AdjacencyMatrix.class, verifier -> verifier.withIgnoredFields("edgeFactory", "nextOffset"));
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
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends Type> type : scanner.getSubTypesOf(Type.class))
			// type token is the only one with an eclipse-like equals
			verify(type, type == TypeTokenType.class);
	}

	@Test
	public void testSymbolicExpressions() {
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends SymbolicExpression> expr : scanner.getSubTypesOf(SymbolicExpression.class))
			if (HeapLocation.class.isAssignableFrom(expr))
				// heap locations use only their name and weakness for equality
				verify(expr, verifier -> verifier.withOnlyTheseFields("name", "weak"));
			else if (Identifier.class.isAssignableFrom(expr))
				// identifiers use only their name for equality
				verify(expr, verifier -> verifier.withOnlyTheseFields("name"));
			else
				verify(expr);
	}

	@Test
	public void testStatements() {
		// suppress nullity: the verifier will try to pass in a code location
		// with null fields (not possible) and this would cause warnings

		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends Statement> st : scanner.getSubTypesOf(Statement.class))
			// the ignored fields are either mutable or auxiliary
			if (Expression.class.isAssignableFrom(st))
				verify(st, verifier -> verifier.withIgnoredFields("cfg", "offset", "runtimeTypes", "parent",
						"metaVariables"), Warning.NULL_FIELDS);
			else
				verify(st, verifier -> verifier.withIgnoredFields("cfg", "offset"), Warning.NULL_FIELDS);
	}

	@Test
	public void testEdges() {
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends Edge> edge : scanner.getSubTypesOf(Edge.class))
			verify(edge);
	}

	@Test
	public void testAnnotations() {
		verify(Annotation.class);
		verify(Annotations.class);
		verify(AnnotationMember.class);

		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends AnnotationValue> value : scanner.getSubTypesOf(AnnotationValue.class))
			verify(value);
		for (Class<? extends AnnotationMatcher> matcher : scanner.getSubTypesOf(AnnotationMatcher.class))
			verify(matcher);
	}

	@Test
	public void testRepresentations() {
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends DomainRepresentation> repr : scanner.getSubTypesOf(DomainRepresentation.class))
			verify(repr);
	}

	@Test
	public void testDomainsAndLattices() {
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
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
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends it.unive.lisa.checks.warnings.Warning> warning : scanner.getSubTypesOf(it.unive.lisa.checks.warnings.Warning.class))
			verify(warning);
	}
	
	@Test
	public void testInterproceduralObjects() {
		verify(CallGraphEdge.class);
		verify(CallGraphNode.class, verifier -> verifier.withIgnoredFields("graph"));
		verify(CFGResults.class, Warning.NONFINAL_FIELDS);
		verify(FixpointResults.class, Warning.NONFINAL_FIELDS);
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
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
		verify(CFGDescriptor.class, Warning.NONFINAL_FIELDS); // 'overridable' is mutable
		verify(VariableTableEntry.class, Warning.NONFINAL_FIELDS); // scope bounds are mutable
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		for (Class<? extends CodeLocation> loc : scanner.getSubTypesOf(CodeLocation.class))
			verify(loc);
		for (Class<? extends ControlFlowStructure> struct : scanner.getSubTypesOf(ControlFlowStructure.class))
			verify(struct, Warning.NONFINAL_FIELDS); // first follower is mutable
		for (Class<? extends Unit> unit : scanner.getSubTypesOf(Unit.class))
			verify(unit, Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED);
		for (Class<? extends CodeMember> cm : scanner.getSubTypesOf(CodeMember.class))
			if (!CFGWithAnalysisResults.class.isAssignableFrom(cm))
				verify(cm, Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED);
	}

	@Test
	@Ignore
	public void testEqualsAndHashCode() throws ClassNotFoundException {
		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner(false));
		Set<String> all = scanner.getAllTypes();
		Class<?> clazz;
		Map<Class<?>, AssertionError> failing = new HashMap<>();
		for (String cls : all) {
			clazz = Class.forName(cls);
			if (!clazz.isAnonymousClass() && !Modifier.isAbstract(clazz.getModifiers())
					&& !Modifier.isInterface(clazz.getModifiers()))
				try {
					System.out.println(clazz.getName());
					EqualsVerifier.simple().forClass(clazz)
							.usingGetClass()
							.suppress(priovideSuppressions(clazz))
							.withIgnoredFields(provideIgnoredFields(clazz))
							.withPrefabValues(CFG.class, cfg1, cfg2)
							.withPrefabValues(CFGDescriptor.class, descr1, descr2)
							.withPrefabValues(CompilationUnit.class, unit1, unit2)
							.withPrefabValues(AdjacencyMatrix.class, adj1, adj2)
							.withPrefabValues(DomainRepresentation.class, dr1, dr2)
							.withPrefabValues(Pair.class, Pair.of(1, 2), Pair.of(3, 4))
							.withPrefabValues(NonInterference.class, new NonInterference().top(),
									new NonInterference().bottom())
							.withPrefabValues(org.graphstream.graph.Graph.class, g1, g2)
							.verify();
				} catch (AssertionError e) {
					failing.put(clazz, e);
				}
		}

		for (Class<?> failed : failing.keySet())
			System.err.println(failed + " failed due to: " + failing.get(failed).getMessage() + "\n");

		if (!failing.isEmpty())
			fail(failing.size() + " classes failed the check. See standard error for details.");
	}

	private static String[] provideIgnoredFields(Class<?> cls) {
		List<String> fields = new ArrayList<>();

		if (Expression.class.isAssignableFrom(cls))
			fields.addAll(List.of("runtimeTypes", "parent", "metaVariables"));

		if (Statement.class.isAssignableFrom(cls))
			fields.addAll(List.of("cfg", "offset"));

		if (cls == InferredPair.class)
			fields.addAll(List.of("domain"));

		if (cls == CallGraphNode.class)
			fields.addAll(List.of("graph"));

		if (cls == AdjacencyMatrix.class)
			fields.addAll(List.of("edgeFactory", "nextOffset"));

		if (DotGraph.class.isAssignableFrom(cls))
			fields.addAll(List.of("legend", "title", "codes", "nextCode"));

		return fields.toArray(String[]::new);
	}

	private static Warning[] priovideSuppressions(Class<?> cls) {
		List<Warning> suppressed = new ArrayList<>();
		// the pattern generated by eclipse uses == for checking against null
		suppressed.add(Warning.REFERENCE_EQUALITY);

		if (cls == LiSA.class
				|| cls == LiSAFactory.class
				|| cls == LiSARunner.class
				|| cls == Caches.class
				|| cls == FileManager.class
				|| cls == ChecksExecutor.class
				|| cls == ContextInsensitiveToken.class
				|| cls == CheckTool.class
				|| cls == CheckToolWithAnalysisResults.class
				|| cls == ControlFlowExtractor.class
				|| cls == Dominators.class
				|| cls == JsonReportComparer.class
				|| cls == CollectionUtilities.class
				|| cls == CollectionsDiffBuilder.class
				|| cls == ExternalSetCache.class
				|| cls.getName().startsWith(ControlFlowExtractor.class.getName() + "$")
				|| CodeMember.class.isAssignableFrom(cls)
				|| Unit.class.isAssignableFrom(cls)
				|| Graph.class.isAssignableFrom(cls)
				|| WorkingSet.class.isAssignableFrom(cls)
				|| InterproceduralAnalysis.class.isAssignableFrom(cls)
				|| CallGraph.class.isAssignableFrom(cls)
				|| ExpressionVisitor.class.isAssignableFrom(cls)
				|| DiffReporter.class.isAssignableFrom(cls)
				|| GraphVisitor.class.isAssignableFrom(cls)
				|| Iterator.class.isAssignableFrom(cls)
				|| cls.getPackageName().startsWith("it.unive.lisa.logging"))
			suppressed.addAll(List.of(Warning.INHERITED_DIRECTLY_FROM_OBJECT, Warning.ALL_FIELDS_SHOULD_BE_USED));
		else if (cls == BitExternalSet.class
				|| Statement.class.isAssignableFrom(cls))
			suppressed.add(Warning.NULL_FIELDS);
		else if (Identifier.class.isAssignableFrom(cls))
			suppressed.add(Warning.ALL_FIELDS_SHOULD_BE_USED);

		return suppressed.toArray(Warning[]::new);
	}
}
