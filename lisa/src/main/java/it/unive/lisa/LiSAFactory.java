package it.unive.lisa;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.nonrelational.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.ValueEnvironment;
import it.unive.lisa.callgraph.CallGraph;

public class LiSAFactory {

	private static <T> T construct(Class<T> component, Class<?>[] argTypes, Object[] params)
			throws AnalysisSetupException {
		try {
			Constructor<T> constructor = component.getConstructor(argTypes);
			return (T) constructor.newInstance(params);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException
				| InvocationTargetException e) {
			throw new AnalysisSetupException("Unable to instantiate " + component.getSimpleName(), e);
		}
	}

	private static Class<?>[] findConstructorSignature(Class<?> component, Object[] params)
			throws AnalysisSetupException {
		Map<Constructor<?>, List<Integer>> candidates = new IdentityHashMap<>();
		Class<?>[] types;
		outer: for (Constructor<?> constructor : component.getConstructors()) {
			types = constructor.getParameterTypes();
			if (params.length != types.length)
				continue;

			List<Integer> toWrap = new ArrayList<>();
			for (int i = 0; i < types.length; i++)
				if (types[i].isAssignableFrom(params[i].getClass()))
					continue;
				else if (needsWrapping(params[i].getClass(), types[i])) {
					toWrap.add(i);
					continue;
				} else
					continue outer;

			candidates.put(constructor, toWrap);
		}

		if (candidates.isEmpty())
			throw new AnalysisSetupException(
					"No suitable constructor of " + component.getSimpleName() + " found for argument types "
							+ Arrays.toString(Arrays.stream(params).map(p -> p.getClass()).toArray(Class[]::new)));

		if (candidates.size() > 1)
			throw new AnalysisSetupException(
					"Constructor call of " + component.getSimpleName() + " is ambiguous for argument types "
							+ Arrays.toString(Arrays.stream(params).map(p -> p.getClass()).toArray(Class[]::new)));

		for (int p : candidates.values().iterator().next())
			params[p] = wrapParam(params[p]);

		return candidates.keySet().iterator().next().getParameterTypes();
	}
	
	private static boolean needsWrapping(Class<?> actual, Class<?> desired) {
		if (NonRelationalHeapDomain.class.isAssignableFrom(actual) && desired.isAssignableFrom(HeapDomain.class)) 
			return true;
		else if (NonRelationalValueDomain.class.isAssignableFrom(actual) && desired.isAssignableFrom(ValueDomain.class)) 
			return true;
		else
			return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object wrapParam(Object param) {
		if (NonRelationalHeapDomain.class.isAssignableFrom(param.getClass()))
			return new HeapEnvironment((NonRelationalHeapDomain<?>) param);
		else if (NonRelationalValueDomain.class.isAssignableFrom(param.getClass()))
			return new ValueEnvironment((NonRelationalValueDomain<?>) param);
		return param;
	}

	public static <T> T getInstance(Class<T> component, Object... params) throws AnalysisSetupException {
		try {
			if (params != null && params.length != 0)
				return (T) construct(component, findConstructorSignature(component, params), params);

			DefaultParameters defaultParams = component.getAnnotation(DefaultParameters.class);
			if (defaultParams == null)
				return (T) construct(component, ArrayUtils.EMPTY_CLASS_ARRAY, ArrayUtils.EMPTY_OBJECT_ARRAY);

			Object[] defaults = new Object[defaultParams.value().length];
			for (int i = 0; i < defaults.length; i++)
				defaults[i] = getInstance(defaultParams.value()[i]);

			return (T) construct(component, findConstructorSignature(component, defaults), defaults);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}

	private static final Map<Class<?>, Class<?>> customDefaults = new HashMap<>();
	
	public static void registerDefaultFor(Class<?> component, Class<?> defaultImplementation) {
		customDefaults.put(component, defaultImplementation);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> getDefaultClassFor(Class<T> component) {
		if (customDefaults.containsKey(component))
			return (Class<? extends T>) customDefaults.get(component);
		DefaultImplementation defaultImpl = component.getAnnotation(DefaultImplementation.class);
		if (defaultImpl == null)
			return null;
		return (Class<? extends T>) defaultImpl.value();
	}

	@SuppressWarnings("unchecked")
	public static <T> T getDefaultFor(Class<T> component, Object... params) throws AnalysisSetupException {
		try {
			Class<? extends T> def = getDefaultClassFor(component);
			if (needsWrapping(def, component)) 
				return (T) wrapParam(getInstance(def, params));
			else 
				return getInstance(def, params);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}

	public static class ConfigurableComponent<T> {
		private static final Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());

		private final Class<T> component;
		private final Class<? extends T> defaultInstance;
		private final Collection<Class<? extends T>> alternatives;

		private ConfigurableComponent(Class<T> component) {
			this.component = component;
			this.defaultInstance = getDefaultClassFor(component);
			this.alternatives = scanner.getSubTypesOf(component)
					.stream()
					.map(c -> Pair.of(c, c.getModifiers()))
					.filter(p -> !Modifier.isAbstract(p.getRight()) && !Modifier.isInterface(p.getRight()))
					.map(p -> p.getLeft())
					.collect(Collectors.toList());
		}

		public Class<T> getComponent() {
			return component;
		}

		public Class<? extends T> getDefaultInstance() {
			return defaultInstance;
		}

		public Collection<Class<? extends T>> getAlternatives() {
			return alternatives;
		}
	}

	public static Collection<ConfigurableComponent<?>> configurableComponents() {
		Collection<ConfigurableComponent<?>> in = new ArrayList<>();
		in.add(new ConfigurableComponent<>(CallGraph.class));
		in.add(new ConfigurableComponent<>(AbstractState.class));
		in.add(new ConfigurableComponent<>(HeapDomain.class));
		in.add(new ConfigurableComponent<>(ValueDomain.class));
		in.add(new ConfigurableComponent<>(NonRelationalHeapDomain.class));
		in.add(new ConfigurableComponent<>(NonRelationalValueDomain.class));
		return in;
	}
}
