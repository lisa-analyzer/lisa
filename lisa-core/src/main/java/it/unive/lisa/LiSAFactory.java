package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
public final class LiSAFactory {

	/**
	 * Default implementation for analysis components. Keys of this map are the
	 * class objects of the analysis components (e.g.
	 * {@link CallGraph}{@code .class}) while values are the key's default
	 * implementation's class object (e.g. {@link RTACallGraph}{@code .class}).
	 */
	static final Map<Class<?>, Class<?>> DEFAULT_IMPLEMENTATIONS = new HashMap<>();

	/**
	 * Default parameters types for analysis components' implementations. Keys
	 * of this map are the class objects of the analysis components (e.g.
	 * {@link SimpleAbstractState}{@code .class}) while values are arrays
	 * containing the key's default parameters' class objects (e.g.
	 * {@code [}{@link MonolithicHeap}{@code .class, }{@link Interval}{@code .class]}).
	 */
	static final Map<Class<?>, Class<?>[]> DEFAULT_PARAMETERS = new HashMap<>();

	static {
		DEFAULT_IMPLEMENTATIONS.put(InterproceduralAnalysis.class, ModularWorstCaseAnalysis.class);
		DEFAULT_IMPLEMENTATIONS.put(CallGraph.class, RTACallGraph.class);
		DEFAULT_IMPLEMENTATIONS.put(HeapDomain.class, MonolithicHeap.class);
		DEFAULT_IMPLEMENTATIONS.put(ValueDomain.class, Interval.class);
		DEFAULT_IMPLEMENTATIONS.put(AbstractState.class, SimpleAbstractState.class);
		DEFAULT_PARAMETERS.put(SimpleAbstractState.class, new Class[] { MonolithicHeap.class, Interval.class });
	}

	private LiSAFactory() {
		// this class is just a static holder
	}

	private static <T> T construct(Class<T> component, Class<?>[] argTypes, Object[] params)
			throws AnalysisSetupException {
		try {
			Constructor<T> constructor = component.getConstructor(argTypes);
			return constructor.newInstance(params);
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
				if (needsWrapping(params[i].getClass(), types[i]))
					toWrap.add(i);
				else if (!types[i].isAssignableFrom(params[i].getClass()))
					continue outer;

			candidates.put(constructor, toWrap);
		}

		if (candidates.isEmpty())
			throw new AnalysisSetupException(
					"No suitable constructor of " + component.getSimpleName() + " found for argument types "
							+ Arrays.toString(Arrays.stream(params).map(Object::getClass).toArray(Class[]::new)));

		if (candidates.size() > 1)
			throw new AnalysisSetupException(
					"Constructor call of " + component.getSimpleName() + " is ambiguous for argument types "
							+ Arrays.toString(Arrays.stream(params).map(Object::getClass).toArray(Class[]::new)));

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
		else if (DataflowElement.class.isAssignableFrom(actual) && desired.isAssignableFrom(ValueDomain.class))
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
		else if (DataflowElement.class.isAssignableFrom(param.getClass())) {
			Class<? extends DataflowElement> elem = (Class<? extends DataflowElement>) param.getClass();
			if (elem.getGenericInterfaces().length == 0)
				return param;

			for (Type gi : elem.getGenericInterfaces())
				if (gi instanceof ParameterizedType && ((ParameterizedType) gi).getRawType() == DataflowElement.class) {
					Type domain = ((ParameterizedType) gi).getActualTypeArguments()[0];
					if (((ParameterizedType) domain).getRawType() == PossibleForwardDataflowDomain.class)
						return new PossibleForwardDataflowDomain((DataflowElement<?, ?>) param);
					else if (((ParameterizedType) domain).getRawType() == DefiniteForwardDataflowDomain.class)
						return new DefiniteForwardDataflowDomain((DataflowElement<?, ?>) param);
					else
						return param;
				}
		}
		return param;
	}

	/**
	 * Creates an instance of the given {@code component}. If {@code params} are
	 * provided, a suitable (and not ambiguous) constructor must exist in
	 * {@code component}'s class. If no {@code params} have been provided but
	 * default parameters have been registered through
	 * {@link #registerDefaultParametersFor(Class, Class...)},
	 * {@link #getInstance(Class, Object...)} will be used to create such
	 * parameters and those will be used. Otherwise, the nullary constructor of
	 * {@code component} is invoked.
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
				return construct(component, findConstructorSignature(component, params), params);

			Class<?>[] defaultParams;
			if (!DEFAULT_PARAMETERS.containsKey(component)
					|| (defaultParams = DEFAULT_PARAMETERS.get(component)) == null
					|| defaultParams.length == 0)
				return construct(component, ArrayUtils.EMPTY_CLASS_ARRAY, ArrayUtils.EMPTY_OBJECT_ARRAY);

			Object[] defaults = new Object[defaultParams.length];
			for (int i = 0; i < defaults.length; i++)
				defaults[i] = getInstance(defaultParams[i]);

			return construct(component, findConstructorSignature(component, defaults), defaults);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}

	/**
	 * Registers a default implementation for {@code component}, taking
	 * precedence over the predefined defaults. Any previous default for
	 * {@code component} introduced by calling this method is removed.
	 * 
	 * @param component             the component whose default implementation
	 *                                  is to be registered
	 * @param defaultImplementation the new default implementation for
	 *                                  {@code component}
	 */
	public static void registerDefaultFor(Class<?> component, Class<?> defaultImplementation) {
		DEFAULT_IMPLEMENTATIONS.put(component, defaultImplementation);
	}

	/**
	 * Registers the types (i.e. implementations) of the parameters to use for
	 * the creation of the given component. These will be used whenever
	 * {@link #getInstance(Class, Object...)} or
	 * {@link #getDefaultFor(Class, Object...)} are invoked without specifying
	 * parameters but a default exist. Any previous default for
	 * {@code component} introduced by calling this method is removed.
	 * 
	 * @param component         the component whose default parameters are to be
	 *                              registered
	 * @param defaultParameters the new parameters for the creation of
	 *                              {@code component}
	 */
	public static void registerDefaultParametersFor(Class<?> component, Class<?>... defaultParameters) {
		DEFAULT_PARAMETERS.put(component, defaultParameters);
	}

	/**
	 * Builds the default instance of the specified analysis component. The
	 * instance to create is retrieved by first looking into the custom defaults
	 * provided through {@link #registerDefaultFor(Class, Class)}. If no entry
	 * for {@code component} has been provided, then the instance is looked up
	 * in the predefined defaults. If {@code component} does not have a
	 * predefined default, then an {@link AnalysisSetupException} is thrown.
	 * Then, {@link #getInstance(Class, Object...)} is invoked on the retrieved
	 * instance, using the given {@code params}. If no {@code params} have been
	 * provided but default parameters have been registered through
	 * {@link #registerDefaultParametersFor(Class, Class...)},
	 * {@link #getInstance(Class, Object...)} will be used to create such
	 * parameters and those will be used. If the default instance is a
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
			Class<?> def = DEFAULT_IMPLEMENTATIONS.get(component);
			if (def == null)
				throw new AnalysisSetupException("No registered default for " + component);

			Class<?>[] defParams = DEFAULT_PARAMETERS.getOrDefault(def, ArrayUtils.EMPTY_CLASS_ARRAY);
			if (params.length == 0 && defParams.length > 0) {
				params = new Object[defParams.length];
				for (int i = 0; i < params.length; i++)
					params[i] = getInstance(defParams[i]);
			}

			if (needsWrapping(def, component))
				return (T) wrapParam(getInstance(def, params));
			else
				return (T) getInstance(def, params);
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
	public static final class ConfigurableComponent<T> {
		private static final Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());

		private final Class<T> component;
		private final Class<? extends T> defaultInstance;
		private final Class<?>[] defaultParameters;
		private final Collection<Class<? extends T>> alternatives;

		@SuppressWarnings("unchecked")
		private ConfigurableComponent(Class<T> component) {
			this.component = component;

			if (DEFAULT_IMPLEMENTATIONS.containsKey(component)) {
				defaultInstance = (Class<? extends T>) DEFAULT_IMPLEMENTATIONS.get(component);
				if (DEFAULT_PARAMETERS.containsKey(defaultInstance))
					defaultParameters = DEFAULT_PARAMETERS.get(defaultInstance);
				else
					defaultParameters = null;
			} else {
				defaultInstance = null;
				defaultParameters = null;
			}

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
		 * {@link #getAlternatives()}. Might be {@code null} if no default is
		 * set.
		 * 
		 * @return the default implementation for this component
		 */
		public Class<? extends T> getDefaultInstance() {
			return defaultInstance;
		}

		/**
		 * Yields the classes of the parameters passed by-default to the
		 * constructor of the default implementation for this component. Might
		 * be {@code null} if no default is set, or might be an empty array if
		 * the default implementation does not require parameters.
		 * 
		 * @return the classes of the parameters for the construction of the
		 *             default implementation for this component
		 */
		public Class<?>[] getDefaultParameters() {
			return defaultParameters;
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((alternatives == null) ? 0 : alternatives.hashCode());
			result = prime * result + ((component == null) ? 0 : component.hashCode());
			result = prime * result + ((defaultInstance == null) ? 0 : defaultInstance.hashCode());
			result = prime * result + ((defaultParameters == null) ? 0 : Arrays.hashCode(defaultParameters));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConfigurableComponent<?> other = (ConfigurableComponent<?>) obj;
			if (alternatives == null) {
				if (other.alternatives != null)
					return false;
			} else if (!alternatives.equals(other.alternatives))
				return false;
			if (component == null) {
				if (other.component != null)
					return false;
			} else if (!component.equals(other.component))
				return false;
			if (defaultInstance == null) {
				if (other.defaultInstance != null)
					return false;
			} else if (!defaultInstance.equals(other.defaultInstance))
				return false;
			if (defaultParameters == null) {
				if (other.defaultParameters != null)
					return false;
			} else if (!Arrays.equals(defaultParameters, other.defaultParameters))
				return false;
			return true;
		}

		@Override
		public String toString() {
			String result = component.getName();
			if (defaultInstance != null) {
				result += " (defaults to: '" + defaultInstance.getName() + "'";
				if (defaultParameters != null && defaultParameters.length != 0) {
					String[] paramNames = Arrays.stream(defaultParameters).map(c -> c.getName()).toArray(String[]::new);
					result += " with parameters [" + StringUtils.join(paramNames, ", ") + "]";
				}
				result += ")";
			}
			String[] alternatives = this.alternatives.stream().map(c -> c.getName()).toArray(String[]::new);
			result += " possible implementations: " + alternatives;
			return result;
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
		in.add(new ConfigurableComponent<>(InterproceduralAnalysis.class));
		in.add(new ConfigurableComponent<>(CallGraph.class));
		in.add(new ConfigurableComponent<>(AbstractState.class));
		in.add(new ConfigurableComponent<>(HeapDomain.class));
		in.add(new ConfigurableComponent<>(ValueDomain.class));
		in.add(new ConfigurableComponent<>(NonRelationalHeapDomain.class));
		in.add(new ConfigurableComponent<>(NonRelationalValueDomain.class));
		in.add(new ConfigurableComponent<>(InferredValue.class));
		in.add(new ConfigurableComponent<>(DataflowElement.class));
		return in;
	}
}
