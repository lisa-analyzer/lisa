package it.unive.lisa.analysis.nonrelational.inference;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.types.IntType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class BaseInferredValueTest {
	
	private static class Sample extends BaseInferredValue<Sample> {

		@Override
		public DomainRepresentation representation() {
			return new StringRepresentation("sample");
		}

		@Override
		public Sample top() {
			return this;
		}

		@Override
		public Sample bottom() {
			return this;
		}

		@Override
		public boolean tracksIdentifiers(Identifier id) {
			return true;
		}

		@Override
		public boolean canProcess(SymbolicExpression expression) {
			return true;
		}

		@Override
		protected Sample lubAux(Sample other) throws SemanticException {
			return this;
		}

		@Override
		protected Sample wideningAux(Sample other) throws SemanticException {
			return this;
		}

		@Override
		protected boolean lessOrEqualAux(Sample other) throws SemanticException {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return Sample.class.hashCode();
		}
	}

	@Test
	public void testDefaults() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Method mtd : BaseInferredValue.class.getDeclaredMethods())
			if (Modifier.isProtected(mtd.getModifiers()))
				try {
					AtomicReference<Integer> envPos = new AtomicReference<>();
					Object[] params = provideParams(mtd, mtd.getParameterTypes(), envPos);
					Object ret = mtd.invoke(new Sample(), params);
					if (mtd.getName().startsWith("eval"))
						assertTrue("Default implementation of " + mtd.getName() + " did not return top",
								((Lattice<?>) ret).isTop());
					else if (mtd.getName().startsWith("satisfies"))
						assertSame("Default implementation of " + mtd.getName() + " did not return UNKNOWN",
								Satisfiability.UNKNOWN, ret);
					else if (mtd.getName().startsWith("assume")) 
						assertSame(
								"Default implementation of " + mtd.getName()
										+ " did not return an unchanged environment",
								params[envPos.get()], ret);
						
				} catch (Exception e) {
					e.printStackTrace();
					fail(mtd + " failed due to " + e.getMessage());
				}
	}

	private static Object[] provideParams(Method mtd, Class<?>[] params, AtomicReference<Integer> envPos) {
		Object[] res = new Object[params.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = provideParam(mtd, params[i]);
			if (params[i] == ValueEnvironment.class)
				envPos.set(i);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private static <R> R provideParam(Method mtd, Class<R> param) {
		class FakePP implements ProgramPoint {

			@Override
			public CodeLocation getLocation() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public CFG getCFG() {
				// TODO Auto-generated method stub
				return null;
			}
			
		}
		
		if (param == ExternalSet.class)
			return (R) Caches.types().mkEmptySet();
		
		if (param == PushAny.class)
			return (R) new PushAny(null, SyntheticLocation.INSTANCE);
		if (param == Constant.class || param == ValueExpression.class)
			return (R) new Constant(IntType.INSTANCE, 5, SyntheticLocation.INSTANCE);
		if (param == Identifier.class)
			return (R) new Variable(provideParam(mtd, ExternalSet.class), "foo", SyntheticLocation.INSTANCE);
		
		if (param == TernaryOperator.class)
			return (R) TernaryOperator.STRING_REPLACE;
		if (param == BinaryOperator.class)
			return (R) BinaryOperator.COMPARISON_EQ;
		if (param == UnaryOperator.class)
			return (R) UnaryOperator.LOGICAL_NOT;
		
		if (param == UnaryExpression.class)
			return (R) new UnaryExpression(provideParam(mtd, ExternalSet.class), provideParam(mtd, Constant.class), provideParam(mtd, UnaryOperator.class), SyntheticLocation.INSTANCE);
		if (param == BinaryExpression.class)
			return (R) new BinaryExpression(provideParam(mtd, ExternalSet.class), provideParam(mtd, Constant.class), provideParam(mtd, Constant.class), provideParam(mtd, BinaryOperator.class), SyntheticLocation.INSTANCE);
		if (param == TernaryExpression.class)
			return (R) new TernaryExpression(provideParam(mtd, ExternalSet.class), provideParam(mtd, Constant.class), provideParam(mtd, Constant.class), provideParam(mtd, Constant.class), provideParam(mtd, TernaryOperator.class), SyntheticLocation.INSTANCE);
		if (param == InferenceSystem.class)
			return (R) new InferenceSystem<>(new Sample());
		if (param == Sample.class || param == BaseInferredValue.class)
			return (R) new Sample();
		if (param == ProgramPoint.class)
			return (R) new FakePP();
		
		throw new UnsupportedOperationException(mtd + ": No default value for type " + param.getName());
	}
}
