package it.unive.lisa.analysis.nonrelational.value;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class BaseNonRelationalTypeDomainTest {

	private static class Sample extends BaseNonRelationalTypeDomain<Sample> {

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
		public Sample lubAux(Sample other) throws SemanticException {
			return this;
		}

		@Override
		public boolean lessOrEqualAux(Sample other) throws SemanticException {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public Set<Type> getRuntimeTypes() {
			return Collections.emptySet();
		}
	}

	@Test
	public void testDefaults() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Method mtd : BaseNonRelationalTypeDomain.class.getDeclaredMethods())
			if (Modifier.isPublic(mtd.getModifiers()) && !isExcluded(mtd))
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

	private static boolean isExcluded(Method mtd) {
		if (mtd.getName().equals("canProcess")
				|| mtd.getName().equals("tracksIdentifiers")
				|| mtd.getName().equals("satisfies")
				|| mtd.getName().equals("assume")
				|| mtd.getName().equals("eval")
				|| mtd.getName().equals("toString"))
			return true;
		return false;
	}

	private static Object[] provideParams(Method mtd, Class<?>[] params, AtomicReference<Integer> envPos) {
		Object[] res = new Object[params.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = provideParam(mtd, params[i]);
			if (params[i] == TypeEnvironment.class)
				envPos.set(i);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private static <R> R provideParam(Method mtd, Class<R> param) {
		class FakePP implements ProgramPoint {

			@Override
			public CodeLocation getLocation() {
				return null;
			}

			@Override
			public CFG getCFG() {
				return null;
			}

			@Override
			public Program getProgram() {
				return new Program(new IMPFeatures(), new IMPTypeSystem());
			}
		}

		if (param == Type.class)
			return (R) Int32Type.INSTANCE;

		if (param == PushAny.class)
			return (R) new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		if (param == Constant.class || param == ValueExpression.class)
			return (R) new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);
		if (param == Identifier.class)
			return (R) new Variable(provideParam(mtd, Type.class), "foo", SyntheticLocation.INSTANCE);

		if (param == TernaryOperator.class)
			return (R) StringReplace.INSTANCE;
		if (param == BinaryOperator.class)
			return (R) ComparisonEq.INSTANCE;
		if (param == UnaryOperator.class)
			return (R) LogicalNegation.INSTANCE;

		if (param == UnaryExpression.class)
			return (R) new UnaryExpression(provideParam(mtd, Type.class), provideParam(mtd, Constant.class),
					provideParam(mtd, UnaryOperator.class), SyntheticLocation.INSTANCE);
		if (param == BinaryExpression.class)
			return (R) new BinaryExpression(provideParam(mtd, Type.class), provideParam(mtd, Constant.class),
					provideParam(mtd, Constant.class), provideParam(mtd, BinaryOperator.class),
					SyntheticLocation.INSTANCE);
		if (param == TernaryExpression.class)
			return (R) new TernaryExpression(provideParam(mtd, Type.class), provideParam(mtd, Constant.class),
					provideParam(mtd, Constant.class), provideParam(mtd, Constant.class),
					provideParam(mtd, TernaryOperator.class), SyntheticLocation.INSTANCE);
		if (param == TypeEnvironment.class)
			return (R) new TypeEnvironment<>(new Sample());
		if (param == Sample.class || param == BaseNonRelationalTypeDomain.class)
			return (R) new Sample();
		if (param == ProgramPoint.class)
			return (R) new FakePP();

		throw new UnsupportedOperationException(mtd + ": No default value for type " + param.getName());
	}
}
