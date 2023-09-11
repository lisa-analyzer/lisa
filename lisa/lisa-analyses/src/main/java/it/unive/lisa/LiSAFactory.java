package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

/**
 * An utility class for instantiating analysis components, that is, modular
 * pieces of the analysis that have several implementations. A specific instance
 * can be retrieved through {@link #getInstance(Class, Object...)}. Moreover,
 * {@link #configurableComponents()} yields a list of each modular LiSA
 * component.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class LiSAFactory {

	private LiSAFactory() {
		// this class is just a static holder
	}

	@SuppressWarnings("unchecked")
	private static <T> T construct(Class<T> component, Class<?>[] argTypes, Object[] params)
			throws AnalysisSetupException {
		if (argTypes.length == 0)
			try {
				// tokens use the getSingleton() pattern for construction
				Method method = component.getMethod("getSingleton");
				if (method != null && Modifier.isStatic(method.getModifiers()))
					return (T) method.invoke(null);
			} catch (NoSuchMethodException e) {
				// we don't do anything: the class does not have a
				// getSingleton()
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new AnalysisSetupException("Unable to instantiate " + component.getSimpleName(), e);
			}

		try {
			Constructor<T> constructor = component.getConstructor(argTypes);
			return constructor.newInstance(params);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
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
		else if (NonRelationalTypeDomain.class.isAssignableFrom(actual) && desired.isAssignableFrom(TypeDomain.class))
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
		else if (NonRelationalTypeDomain.class.isAssignableFrom(param.getClass()))
			return new TypeEnvironment((NonRelationalTypeDomain<?>) param);
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
	 * {@code component}'s class. Otherwise, the nullary constructor of
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

			return construct(component, ArrayUtils.EMPTY_CLASS_ARRAY, ArrayUtils.EMPTY_OBJECT_ARRAY);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}

	/**
	 * An analysis component that can be configured, that is, it has more than
	 * one implementation that can be modularly integrated into the analysis.
	 * {@link #getComponent()} yields the component itself, i.e. the interface
	 * or abstract class that defines the analysis component.
	 * {@link #getAlternatives()} yields all the concrete implementations of the
	 * component. <br>
	 * <br>
	 * Note that all information present in instances of this class reflect the
	 * information known to LiSA when this component is created, that is, what
	 * is present inside the classpath and already loaded by the JVM.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class ConfigurableComponent {
		private static final Reflections scanner = new Reflections("", new SubTypesScanner());

		private final Class<?> component;
		private final Set<Class<?>> alternatives;

		private ConfigurableComponent(Class<?> component) {
			this.component = component;
			this.alternatives = new HashSet<>();

			@SuppressWarnings("rawtypes")
			Set subtypes = scanner.getSubTypesOf(component);
			for (Object sub : subtypes) {
				Class<?> subtype = (Class<?>) sub;
				if (Modifier.isAbstract(subtype.getModifiers()) || subtype.isInterface())
					continue;

				alternatives.add(subtype);
			}
		}

		/**
		 * Yields the component represented by this
		 * {@link ConfigurableComponent}.
		 * 
		 * @return the analysis component
		 */
		public Class<?> getComponent() {
			return component;
		}

		/**
		 * Yields the alternatives for this component, that is, the concrete
		 * classes that implements it. Each alternative is mapped to its default
		 * parameters, if any.
		 * 
		 * @return the alternatives for this component
		 */
		public Set<Class<?>> getAlternatives() {
			return alternatives;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((alternatives == null) ? 0 : alternatives.hashCode());
			result = prime * result + ((component == null) ? 0 : component.hashCode());
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
			ConfigurableComponent other = (ConfigurableComponent) obj;
			if (component == null) {
				if (other.component != null)
					return false;
			} else if (!component.equals(other.component))
				return false;
			if (alternatives == null) {
				if (other.alternatives != null)
					return false;
			} else if (!alternatives.equals(other.alternatives))
				return false;
			return true;
		}

		@Override
		public String toString() {
			String result = component.getName();
			String[] alternatives = this.alternatives.stream()
					.map(e -> e.getName())
					.toArray(String[]::new);
			result += " possible implementations: " + StringUtils.join(alternatives, ", ");
			return result;
		}
	}

	/**
	 * Yields the collection of {@link ConfigurableComponent}s that can be used
	 * to customize the analysis.<br>
	 * <br>
	 * Note that all information present in the returned instances reflect the
	 * information known to LiSA when this component is created, that is, what
	 * is present inside the classpath and already loaded by the JVM.
	 * 
	 * @return the components that can be configured
	 */
	public static Collection<ConfigurableComponent> configurableComponents() {
		Collection<ConfigurableComponent> in = new ArrayList<>();
		in.add(new ConfigurableComponent(InterproceduralAnalysis.class));
		in.add(new ConfigurableComponent(CallGraph.class));
		in.add(new ConfigurableComponent(AbstractState.class));
		in.add(new ConfigurableComponent(HeapDomain.class));
		in.add(new ConfigurableComponent(ValueDomain.class));
		in.add(new ConfigurableComponent(TypeDomain.class));
		in.add(new ConfigurableComponent(NonRelationalHeapDomain.class));
		in.add(new ConfigurableComponent(NonRelationalValueDomain.class));
		in.add(new ConfigurableComponent(NonRelationalTypeDomain.class));
		in.add(new ConfigurableComponent(InferredValue.class));
		in.add(new ConfigurableComponent(DataflowElement.class));
		return in;
	}
}
