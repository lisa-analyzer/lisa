package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.callgraph.CallGraph;
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

/**
 * An utility class for instantiating analysis components, that is, modular
 * pieces of the analysis that have several implementations. The default
 * instance for a component can be retrieved through
 * {@link #getDefaultFor(Class, Object...)}, while a specific instance can be
 * retrieved through {@link #getInstance(Class, Object...)}. Note that custom
 * defaults for each component can be defined through
 * {@link #registerDefaultFor(Class, Class)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
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
		else if (InferredValue.class.isAssignableFrom(actual) && desired.isAssignableFrom(ValueDomain.class))
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
		else if (InferredValue.class.isAssignableFrom(param.getClass()))
			return new InferenceSystem((InferredValue<?>) param);
		return param;
	}

	/**
	 * Creates an instance of the given {@code component}. If {@code params} are
	 * provided, a suitable constructor (and not ambiguous) must exist in
	 * {@code component}'s class. Otherwise, {@code component}'s class in
	 * inspected for the presence of a {@link DefaultParameters} annotations. If
	 * found, the instance will be created by passing to the constructor
	 * instances of those parameters obtained through
	 * {@link #getInstance(Class, Object...)} without passing any
	 * {@code params}. Otherwise, the nullary constructor of {@code component}
	 * is invoked.
	 * 
	 * @param <T>       the type of the component
	 * @param component the component to instantiate
	 * @param params    the parameters for the creation
	 * 
	 * @return an instance of the given component
	 * 
	 * @throws AnalysisSetupException if the component cannot be created
	 */
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

	/**
	 * Registers a default implementation for {@code component}, taking
	 * precedence over the defaults defined through
	 * {@link DefaultImplementation}. Any previous default for {@code component}
	 * introduced by calling this method is removed.
	 * 
	 * @param component             the component whose default implementation
	 *                                  is to be registered
	 * @param defaultImplementation the new default implementation for
	 *                                  {@code component}
	 */
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

	/**
	 * Builds the default instance of the specified analysis component. The
	 * instance to create is retrieved by first looking into the custom defaults
	 * provided through {@link #registerDefaultFor(Class, Class)} If no entry
	 * for {@code component} has been provided, then the instance from the
	 * {@code component}'s {@link DefaultImplementation} annotation. If
	 * {@code component} is not annotated with such an annotation, then an
	 * {@link AnalysisSetupException} is thrown. Then,
	 * {@link #getInstance(Class, Object...)} is invoked on the retrieved
	 * instance, using the given {@code params}. If the default instance is a
	 * {@link NonRelationalDomain} and the component is a {@link HeapDomain} or
	 * {@link ValueDomain}, then the instance is wrapped into the appropriate
	 * environment (either {@link HeapEnvironment} or {@link ValueEnvironment})
	 * before being returned.
	 * 
	 * @param <T>       the type of the component
	 * @param component the component to instantiate
	 * @param params    the parameters for the creation of the default instance
	 * 
	 * @return an instance of the default implementation of the given component
	 * 
	 * @throws AnalysisSetupException if the default implementation cannot be
	 *                                    created
	 */
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

	/**
	 * An analysis component that can be configured, that is, it has more than
	 * one implementation that can be modularly integrated into the analysis.
	 * {@link #getComponent()} yields the component itself, i.e. the interface
	 * or abstract class that defines the analysis components.
	 * {@link #getDefaultInstance()} yields the default implementation of the
	 * component that will be used if no specific implementation is requested.
	 * {@link #getAlternatives()} yields all the concrete implementations of the
	 * components.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <T> the type of the component
	 */
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

		/**
		 * Yields the component represented by this
		 * {@link ConfigurableComponent}.
		 * 
		 * @return the analysis component
		 */
		public Class<T> getComponent() {
			return component;
		}

		/**
		 * Yields the default implementation for this component, that is, the
		 * concrete class that implements it and that will be used if the
		 * component is requested but the user did not specify which
		 * implementation to use (among the ones offered by
		 * {@link #getAlternatives()}.
		 * 
		 * @return the default implementation for this component
		 */
		public Class<? extends T> getDefaultInstance() {
			return defaultInstance;
		}

		/**
		 * Yields the alternatives for this component, that is, the concrete
		 * classes that implements it.
		 * 
		 * @return the alternatives for this component
		 */
		public Collection<Class<? extends T>> getAlternatives() {
			return alternatives;
		}
	}

	/**
	 * Yields the collection of {@link ConfigurableComponent}s that can be used
	 * to customize the analysis.
	 * 
	 * @return the components that can be configured
	 */
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
