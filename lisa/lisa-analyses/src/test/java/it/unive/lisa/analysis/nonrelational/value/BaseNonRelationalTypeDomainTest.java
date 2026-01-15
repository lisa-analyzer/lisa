package it.unive.lisa.analysis.nonrelational.value;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.nonrelational.type.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class BaseNonRelationalTypeDomainTest {

	@Test
	public void testDefaults()
			throws IllegalAccessException,
			IllegalArgumentException,
			InvocationTargetException {
		for (Method mtd : BaseNonRelationalTypeDomain.class.getDeclaredMethods())
			if (Modifier.isPublic(mtd.getModifiers()) && !isExcluded(mtd))
				try {
					AtomicReference<Integer> envPos = new AtomicReference<>();
					Object[] params = TestParameterProvider
							.provideParams(mtd, mtd.getParameterTypes(), TypeEnvironment.class, envPos);
					Object ret = mtd.invoke(new TestParameterProvider.SampleNRTD(), params);
					if (mtd.getName().startsWith("eval"))
						assertTrue(
								"Default implementation of " + mtd.getName() + " did not return top",
								((Lattice<?>) ret).isTop());
					else if (mtd.getName().startsWith("satisfies"))
						assertSame(
								"Default implementation of " + mtd.getName() + " did not return UNKNOWN",
								Satisfiability.UNKNOWN,
								ret);
					else if (mtd.getName().startsWith("assume"))
						assertSame(
								"Default implementation of " + mtd.getName()
										+ " did not return an unchanged environment",
								params[envPos.get()],
								ret);

				} catch (Exception e) {
					e.printStackTrace();
					fail(mtd + " failed due to " + e.getMessage());
				}
	}

	private static boolean isExcluded(
			Method mtd) {
		if (mtd.getName().equals("canProcess")
				|| mtd.getName().equals("tracksIdentifiers")
				|| mtd.getName().equals("satisfies")
				|| mtd.getName().equals("assume")
				|| mtd.getName().equals("eval")
				|| mtd.getName().equals("toString"))
			return true;
		return false;
	}

}
