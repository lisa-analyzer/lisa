package it.unive.lisa;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.inference.BaseInferredValue;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
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
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class TestParameterProvider {
	private TestParameterProvider() {
	}

	public static class SampleNRVD implements BaseNonRelationalValueDomain<SampleNRVD> {

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("sample");
		}

		@Override
		public SampleNRVD top() {
			return this;
		}

		@Override
		public SampleNRVD bottom() {
			return this;
		}

		@Override
		public SampleNRVD lubAux(
				SampleNRVD other)
				throws SemanticException {
			return this;
		}

		@Override
		public boolean lessOrEqualAux(
				SampleNRVD other)
				throws SemanticException {
			return true;
		}

		@Override
		public boolean equals(
				Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	}

	public static class SampleIV implements BaseInferredValue<SampleIV> {

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("sample");
		}

		@Override
		public SampleIV top() {
			return this;
		}

		@Override
		public SampleIV bottom() {
			return this;
		}

		@Override
		public boolean canProcess(
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return true;
		}

		@Override
		public SampleIV lubAux(
				SampleIV other)
				throws SemanticException {
			return this;
		}

		@Override
		public boolean lessOrEqualAux(
				SampleIV other)
				throws SemanticException {
			return true;
		}

		@Override
		public boolean equals(
				Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return SampleIV.class.hashCode();
		}
	}

	public static class SampleNRTD implements BaseNonRelationalTypeDomain<SampleNRTD> {

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("sample");
		}

		@Override
		public SampleNRTD top() {
			return this;
		}

		@Override
		public SampleNRTD bottom() {
			return this;
		}

		@Override
		public SampleNRTD lubAux(
				SampleNRTD other)
				throws SemanticException {
			return this;
		}

		@Override
		public boolean lessOrEqualAux(
				SampleNRTD other)
				throws SemanticException {
			return true;
		}

		@Override
		public boolean equals(
				Object obj) {
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

	public static class FakePP implements ProgramPoint {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
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

	public static Object[] provideParams(
			Method mtd,
			Class<?>[] params,
			Class<?> envClass,
			AtomicReference<Integer> envPos) {
		Object[] res = new Object[params.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = provideParam(mtd, params[i]);
			if (params[i] == envClass)
				envPos.set(i);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <R> R provideParam(
			Method mtd,
			Class<R> param) {
		if (param == Type.class)
			return (R) Int32Type.INSTANCE;

		if (param == PushAny.class)
			return (R) new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		if (param == PushInv.class)
			return (R) new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		if (param == Constant.class || param == ValueExpression.class)
			return (R) new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);
		if (param == Identifier.class)
			return (R) new Variable(provideParam(mtd, Type.class), "foo", SyntheticLocation.INSTANCE);
		if (param == Skip.class)
			return (R) new Skip(SyntheticLocation.INSTANCE);

		if (param == TernaryOperator.class)
			return (R) StringReplace.INSTANCE;
		if (param == BinaryOperator.class)
			return (R) ComparisonEq.INSTANCE;
		if (param == UnaryOperator.class)
			return (R) LogicalNegation.INSTANCE;

		if (param == UnaryExpression.class)
			return (R) new UnaryExpression(
					provideParam(mtd, Type.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, UnaryOperator.class),
					SyntheticLocation.INSTANCE);
		if (param == BinaryExpression.class)
			return (R) new BinaryExpression(
					provideParam(mtd, Type.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, BinaryOperator.class),
					SyntheticLocation.INSTANCE);
		if (param == TernaryExpression.class)
			return (R) new TernaryExpression(
					provideParam(mtd, Type.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, Constant.class),
					provideParam(mtd, TernaryOperator.class),
					SyntheticLocation.INSTANCE);

		if (param == ValueEnvironment.class)
			return (R) new ValueEnvironment<>(new SampleNRVD());
		if (param == SampleNRVD.class || param == BaseNonRelationalValueDomain.class)
			return (R) new SampleNRVD();

		if (param == InferenceSystem.class)
			return (R) new InferenceSystem<>(new SampleIV());
		if (param == SampleIV.class || param == BaseInferredValue.class)
			return (R) new SampleIV();

		if (param == TypeEnvironment.class)
			return (R) new TypeEnvironment<>(new SampleNRTD());
		if (param == SampleNRTD.class || param == BaseNonRelationalTypeDomain.class)
			return (R) new SampleNRTD();

		if (param == SemanticOracle.class)
			return (R) DefaultConfiguration.defaultAbstractState();

		if (param == ProgramPoint.class)
			return (R) new FakePP();

		throw new UnsupportedOperationException(mtd + ": No default value for type " + param.getName());
	}
}
