package it.unive.lisa;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.nonrelational.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.ValueEnvironment;

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
				else if (NonRelationalHeapDomain.class.isAssignableFrom(params[i].getClass())
						&& types[i].isAssignableFrom(HeapDomain.class)) {
					toWrap.add(i);
					continue;
				} else if (NonRelationalValueDomain.class.isAssignableFrom(params[i].getClass())
						&& types[i].isAssignableFrom(ValueDomain.class)) {
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

			Class<?>[] types = new Class[defaultParams.value().length];
			Object[] defaults = new Object[defaultParams.value().length];
			for (int i = 0; i < defaults.length; i++) {
				DefaultParameter par = defaultParams.value()[i];
				types[i] = par.type();
				defaults[i] = getInstance(par.value());
			}

			return (T) construct(component, types, defaults);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getDefaultFor(Class<T> component, Object... params) throws AnalysisSetupException {
		try {
			return getInstance((Class<T>) component.getAnnotation(DefaultImplementation.class).value(), params);
		} catch (NullPointerException e) {
			throw new AnalysisSetupException("Unable to instantiate default " + component.getSimpleName(), e);
		}
	}
}
