package it.unive.lisa.program;

import static org.junit.Assert.fail;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.type.Type;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class AccessModifiers {

	@Test
	public void testVisibilityOfSymbolicOperators() {
		Set<String> failures = new HashSet<>();
		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Operator>> operators = scanner.getSubTypesOf(Operator.class);
		for (Class<? extends Operator> operator : operators)
			if (!Modifier.isAbstract(operator.getModifiers())) {
				for (Constructor<?> c : operator.getDeclaredConstructors())
					try {
						if (c.getParameterTypes().length == 0
								&& !Modifier.isPublic(c.getModifiers())
								&& !Modifier.isProtected(c.getModifiers()))
							failures.add(operator.getName());
					} catch (Exception e) {
						failures.add(operator.getName());
					}
			}

		if (!failures.isEmpty())
			fail("The following operators do not have a visible constructor: " + failures);
	}

	@Test
	public void testVisibilityOfTypes() {
		Set<String> failures = new HashSet<>();
		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Type>> types = scanner.getSubTypesOf(Type.class);
		for (Class<? extends Type> type : types)
			if (!Modifier.isAbstract(type.getModifiers())) {
				for (Constructor<?> c : type.getDeclaredConstructors())
					try {
						if (c.getParameterTypes().length == 0
								&& (c.getModifiers() & Modifier.PUBLIC) == 0
								&& (c.getModifiers() & Modifier.PROTECTED) == 0)
							failures.add(type.getName());
					} catch (Exception e) {
						failures.add(type.getName());
					}
			}

		if (!failures.isEmpty())
			fail("The following expressions do not have a visible constructor: " + failures);
	}
}
