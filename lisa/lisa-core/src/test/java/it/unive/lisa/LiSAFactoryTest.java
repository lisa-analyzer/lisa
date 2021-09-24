package it.unive.lisa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.LiSAFactory.ConfigurableComponent;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.symbolic.value.Variable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class LiSAFactoryTest {

	private static final Collection<ConfigurableComponent<?>> components = LiSAFactory.configurableComponents();

	@Test
	public void ensureDefaultsConsistency() {
		Collection<ConfigurableComponent<?>> getDefault = new ArrayList<>();
		Collection<ConfigurableComponent<?>> getInstanceOfDefault = new ArrayList<>();
		Map<Class<?>, Class<?>[]> getInstanceWithDefaultParams = new HashMap<>();
		for (ConfigurableComponent<?> comp : components)
			if (comp.getDefaultInstance() != null) {
				try {
					LiSAFactory.getDefaultFor(comp.getComponent());
				} catch (AnalysisSetupException e) {
					getDefault.add(comp);
				}

				try {
					LiSAFactory.getInstance(comp.getDefaultInstance());
				} catch (AnalysisSetupException e) {
					getInstanceOfDefault.add(comp);
				}
			}

		for (Entry<Class<?>, Class<?>[]> impl : LiSAFactory.DEFAULT_PARAMETERS.entrySet())
			try {
				LiSAFactory.getInstance(impl.getKey());
				Object[] params = new Object[impl.getValue().length];
				for (int i = 0; i < params.length; i++)
					params[i] = LiSAFactory.getInstance(impl.getValue()[i]);
				LiSAFactory.getInstance(impl.getKey(), params);
			} catch (AnalysisSetupException e) {
				getInstanceWithDefaultParams.put(impl.getKey(), impl.getValue());
			}

		if (!getDefault.isEmpty()) {
			System.err.println(
					"The following default implementations cannot be created through LiSAFactory.getDefaultFor(...): ");
			for (ConfigurableComponent<?> comp : getDefault)
				System.err.println("  - " + comp.getDefaultInstance().getName() + " (default for: "
						+ comp.getComponent().getName() + ")");
		}

		if (!getInstanceOfDefault.isEmpty()) {
			System.err.println(
					"The following default implementations cannot be created through LiSAFactory.getInstance(...): ");
			for (ConfigurableComponent<?> comp : getInstanceOfDefault)
				System.err.println("  - " + comp.getDefaultInstance().getName() + " (default for: "
						+ comp.getComponent().getName() + ")");
		}

		if (!getInstanceWithDefaultParams.isEmpty()) {
			System.err.println(
					"The following implementations have default parameters in LiSAFactory.ANALYSIS_DEFAULTS and cannot be created through LiSAFactory.getInstance(...) using those parameters: ");
			for (Class<?> alt : getInstanceWithDefaultParams.keySet())
				System.err.println("  - " + alt.getName() + " (with params: "
						+ StringUtils.join(getInstanceWithDefaultParams.get(alt), ", ") + ")");
		}

		assertTrue("Problems creating instances",
				getDefault.isEmpty() && getInstanceOfDefault.isEmpty() && getInstanceWithDefaultParams.isEmpty());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testCustomDefaults() throws AnalysisSetupException {
		Class<ValueDomain> target = ValueDomain.class;
		Class<?> newDefault = Sign.class;
		Class<?> oldDefault = removeEnvironment(target);
		assertNotEquals("Old and new defaults are the same", oldDefault, newDefault);

		String message = "Setting custom default for " + target.getName() + " to " + newDefault.getName()
				+ " didn't have any effect on %s";
		LiSAFactory.registerDefaultFor(target, newDefault);

		assertSame(String.format(message, "LiSAFactory.getDefaultFor(...)"), newDefault, removeEnvironment(target));

		for (ConfigurableComponent<?> comp : LiSAFactory.configurableComponents())
			if (comp.getComponent() == target)
				assertEquals(String.format(message, "LiSAFactory.configurableComponents()"), newDefault,
						comp.getDefaultInstance());
	}

	private Class<?> removeEnvironment(Class<?> target) throws AnalysisSetupException {
		Object def = LiSAFactory.getDefaultFor(target);

		// by getting top(), we know that whatever variable we ask for, we will
		// be getting the top instance of the inner lattice
		if (def instanceof ValueEnvironment<?>)
			def = ((ValueEnvironment<?>) def).top()
					.getState(
							new Variable(Caches.types().mkEmptySet(), "foo", new SourceCodeLocation("unknown", 0, 0)));
		else if (def instanceof HeapEnvironment<?>)
			def = ((HeapEnvironment<?>) def).top()
					.getState(
							new Variable(Caches.types().mkEmptySet(), "foo", new SourceCodeLocation("unknown", 0, 0)));

		return def.getClass();
	}
}
