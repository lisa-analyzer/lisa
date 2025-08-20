package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class SemanticsSanityTest {

	@Test
	public void testSemanticsOfStatements() {
		Map<Class<? extends Statement>, Map<String, Exception>> failures = new HashMap<>();
		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Statement>> statements = scanner.getSubTypesOf(Statement.class);
		int total = 0;
		for (Class<? extends Statement> statement : statements)
			if (!Modifier.isAbstract(statement.getModifiers()) && !excluded(statement)) {
				total++;
				for (Constructor<?> c : statement.getConstructors())
					try {
						Class<?>[] types = c.getParameterTypes();
						Object[] params = new Object[types.length];
						for (int i = 0; i < params.length; i++)
							params[i] = TestParameterProvider.provideParam(c, types[i]);
						Statement st = (Statement) c.newInstance(params);
						st.forwardSemantics(
								TestParameterProvider.as,
								TestParameterProvider.interprocedural,
								TestParameterProvider.store);
					} catch (Exception e) {
						failures.computeIfAbsent(statement, s -> new HashMap<>())
								.put(Arrays.toString(c.getParameterTypes()), e);
					}
			}

		for (Entry<Class<? extends Statement>, Map<String, Exception>> entry : failures.entrySet())
			for (Entry<String, Exception> e : entry.getValue().entrySet()) {
				System.err.println(entry.getKey() + " failed for " + e.getKey() + " due to " + e.getValue());
				e.getValue().printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " semantics evaluation failed");
	}

	private boolean excluded(
			Class<? extends Statement> statement) {
		// these just forward the semantics to the inner call
		return statement == MultiCall.class || statement == TruncatedParamsCall.class;
	}

	@SuppressWarnings("unchecked")
	private <O, T extends O> int buildDomainsInstances(
			Set<Class<? extends T>> classes,
			Set<O> instances,
			List<String> failures) {
		int total = 0;
		Constructor<?> nullary, unary, binary, ternary, quaternary;
		nullary = unary = binary = ternary = quaternary = null;
		T instance;
		for (Class<? extends T> clazz : classes)
			if (!Modifier.isAbstract(clazz.getModifiers())
					&& !Modifier.isInterface(clazz.getModifiers())
					&& !clazz.isAnonymousClass()
					&& !Satisfiability.class.isAssignableFrom(clazz)
					&& !CompoundState.class.isAssignableFrom(clazz)
					// some testing domain that we do not care about end up here
					&& !clazz.getName().contains("Test")) {
				total++;
				for (Constructor<?> c : clazz.getConstructors()) {
					if (c.getParameterCount() == 0)
						nullary = c;
					else if (c.getParameterCount() == 1)
						unary = c;
					else if (c.getParameterCount() == 2)
						binary = c;
					else if (c.getParameterCount() == 3)
						ternary = c;
					else if (c.getParameterCount() == 4)
						quaternary = c;
				}
				try {
					instance = (T) TestParameterProvider.build(clazz);
					if (instance != null) {
						instances.add(instance);
						nullary = unary = binary = ternary = quaternary = null;
						continue;
					}
					if (nullary != null)
						instance = (T) nullary.newInstance();
					else if (unary != null) {
						Class<?>[] types = unary.getParameterTypes();
						Object param = TestParameterProvider.domainFor(clazz, types[0]);
						instance = (T) unary.newInstance(param);
					} else if (binary != null) {
						Class<?>[] types = binary.getParameterTypes();
						Object param1 = TestParameterProvider.domainFor(clazz, types[0]);
						Object param2 = TestParameterProvider.domainFor(clazz, types[1]);
						instance = (T) binary.newInstance(param1, param2);
					} else if (ternary != null) {
						Class<?>[] types = ternary.getParameterTypes();
						Object param1 = TestParameterProvider.domainFor(clazz, types[0]);
						Object param2 = TestParameterProvider.domainFor(clazz, types[1]);
						Object param3 = TestParameterProvider.domainFor(clazz, types[2]);
						instance = (T) ternary.newInstance(param1, param2, param3);
					} else if (quaternary != null) {
						Class<?>[] types = quaternary.getParameterTypes();
						Object param1 = TestParameterProvider.domainFor(clazz, types[0]);
						Object param2 = TestParameterProvider.domainFor(clazz, types[1]);
						Object param3 = TestParameterProvider.domainFor(clazz, types[2]);
						Object param4 = TestParameterProvider.domainFor(clazz, types[3]);
						instance = (T) quaternary.newInstance(param1, param2, param3, param4);
					} else {
						failures.add(clazz.getName());
						System.err.println("No suitable consturctor found for " + clazz.getName());
						continue;
					}

					instances.add(instance);

					nullary = unary = binary = ternary = quaternary = null;
				} catch (Exception e) {
					failures.add(clazz.getName());
					System.err.println("Instantiation of class " + clazz.getName() + " failed due to " + e);
					e.printStackTrace(System.err);
				}
			}

		return total;
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testAssignOnBottom() {
		List<String> failures = new ArrayList<>();

		BoolType bool = BoolType.INSTANCE;

		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends SemanticDomain>> domains = scanner.getSubTypesOf(SemanticDomain.class);
		Set<Object> instances = new HashSet<>();
		int total = buildDomainsInstances(domains, instances, failures);
		scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends SemanticComponent>> components = scanner.getSubTypesOf(SemanticComponent.class);
		total += buildDomainsInstances(components, instances, failures);

		Variable v = new Variable(bool, "b", SyntheticLocation.INSTANCE);
		PushAny arg = new PushAny(bool, SyntheticLocation.INSTANCE);
		for (Object instance : instances)
			try {
				Object res;
				if (instance instanceof SemanticDomain) {
					SemanticDomain dom = (SemanticDomain) instance;
					DomainLattice l = (DomainLattice) dom.makeLattice().bottom();
					res = dom.assign(l, v, arg, TestParameterProvider.provideParam(null, ProgramPoint.class));
				} else {
					SemanticComponent dom = (SemanticComponent) instance;
					DomainLattice l = (DomainLattice) dom.makeLattice().bottom();
					res = dom.assign(
							l,
							v,
							arg,
							TestParameterProvider.provideParam(null, ProgramPoint.class),
							TestParameterProvider.provideParam(null, SemanticOracle.class));
				}
				if (res instanceof Pair)
					// heap domains return a pair of (state, replacements)
					res = ((Pair) res).getLeft();
				boolean isBottom = ((Lattice) res).isBottom();
				if (res instanceof AnalysisState) {
					AnalysisState state = (AnalysisState) res;
					isBottom = state.getState().isBottom()
							&& state.getFixpointInformation() != null
							&& state.getFixpointInformation().isBottom()
							// analysis state keeps the assigned id on the stack
							&& state.getComputedExpressions().size() == 1
							&& state.getComputedExpressions().iterator().next().equals(v);
				}

				if (!isBottom) {
					failures.add(instance.getClass().getName());
					System.err.println(
							"Assigning to the bottom instance of "
									+ instance.getClass().getName()
									+ " returned a non-bottom instance");
				}
			} catch (Exception e) {
				failures.add(instance.getClass().getName());
				System.err.println("assignOnBottom: " + instance.getClass().getName() + " failed due to " + e);
				e.printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " assignments failed");
	}

	@Test
	@SuppressWarnings({ "rawtypes" })
	public void testIsTopIsBottom() {
		List<String> failures = new ArrayList<>();

		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Lattice>> domains = scanner.getSubTypesOf(Lattice.class);

		Set<Lattice> instances = new HashSet<>();
		int total = buildDomainsInstances(domains, instances, failures);

		for (Lattice instance : instances)
			try {
				if (!instance.bottom().isBottom()) {
					failures.add(instance.getClass().getName());
					System.err.println("bottom().isBottom() returned false on " + instance.getClass().getName());
				}

				if (!instance.top().isTop()) {
					failures.add(instance.getClass().getName());
					System.err.println("top().isTop() returned false on " + instance.getClass().getName());
				}
			} catch (Exception e) {
				failures.add(instance.getClass().getName());
				System.err.println("isTopIsBottom: " + instance.getClass().getName() + " failed due to " + e);
				e.printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " tests failed");
	}

}
