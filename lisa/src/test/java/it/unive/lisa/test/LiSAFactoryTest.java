package it.unive.lisa.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.DefaultParameters;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.LiSAFactory.ConfigurableComponent;

public class LiSAFactoryTest {

	private static final Collection<ConfigurableComponent<?>> components = LiSAFactory.configurableComponents();

	@Test
	public void ensureDefaultsConsistency() {
		Collection<ConfigurableComponent<?>> getDefault = new ArrayList<>();
		Collection<ConfigurableComponent<?>> getInstanceOfDefault = new ArrayList<>();
		Map<Class<?>, ConfigurableComponent<?>> getInstanceWithDefaultParams = new HashMap<>();
		for (ConfigurableComponent<?> comp : components) {
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

			for (Class<?> alt : comp.getAlternatives())
				if (alt.isAnnotationPresent(DefaultParameters.class))
					try {
						LiSAFactory.getInstance(alt);
					} catch (AnalysisSetupException e) {
						getInstanceWithDefaultParams.put(alt, comp);
					}
		}
		
		if (!getDefault.isEmpty()) {
			System.err.println("The following default implementations cannot be created through LiSAFactory.getDefaultFor(...): ");
			for (ConfigurableComponent<?> comp : getDefault)
				System.err.println("  - " + comp.getDefaultInstance().getName() + " (default for: " + comp.getComponent().getName() + ")");
		}
		
		if (!getInstanceOfDefault.isEmpty()) {
			System.err.println("The following default implementations cannot be created through LiSAFactory.getInstance(...): ");
			for (ConfigurableComponent<?> comp : getInstanceOfDefault)
				System.err.println("  - " + comp.getDefaultInstance().getName() + " (default for: " + comp.getComponent().getName() + ")");
		}
		
		if (!getInstanceWithDefaultParams.isEmpty()) {
			System.err.println("The following alternatives that are annotated with @DefaultParameters cannot be created through LiSAFactory.getInstance(...) relying on the information from the annotation: ");
			for (Class<?> alt : getInstanceWithDefaultParams.keySet())
				System.err.println("  - " + alt.getName() + " (alternative for: " + getInstanceWithDefaultParams.get(alt).getComponent().getName() + ")");
		}

		assertTrue("Problems creating instances",
				getDefault.isEmpty() && getInstanceOfDefault.isEmpty() && getInstanceWithDefaultParams.isEmpty());
	}
}
