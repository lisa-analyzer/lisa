package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.INT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

// covers every concrete class extending StringOperation: they differ only in
// toString() and resultType(), with typeInference() itself defined once on
// the shared base and exercised here through each subclass
public class StringOperationTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	private static class Case {

		final StringOperation operator;
		final String symbol;
		final Function<OperatorTestFixtures.FakeTypeSystem, Type> resultType;

		Case(
				StringOperation operator,
				String symbol,
				Function<OperatorTestFixtures.FakeTypeSystem, Type> resultType) {
			this.operator = operator;
			this.symbol = symbol;
			this.resultType = resultType;
		}
	}

	private static final List<Case> CASES = Arrays.asList(
			new Case(StringConcat.INSTANCE, "strcat", ts -> STR),
			new Case(StringContains.INSTANCE, "strcontains", ts -> BOOL),
			new Case(StringEndsWith.INSTANCE, "strends", ts -> BOOL),
			new Case(StringStartsWith.INSTANCE, "strstarts", ts -> BOOL),
			new Case(StringIsPrefixOf.INSTANCE, "strisprefix", ts -> BOOL),
			new Case(StringIsSuffixOf.INSTANCE, "strisuffix", ts -> BOOL),
			new Case(StringEquals.INSTANCE, "strcmp", ts -> BOOL),
			new Case(StringEqualsIgnoreCase.INSTANCE, "stricmp", ts -> BOOL),
			new Case(StringMatches.INSTANCE, "strmatches", ts -> BOOL),
			new Case(StringIndexOf.INSTANCE, "strindexof", ts -> INT32),
			new Case(StringLastIndexOf.INSTANCE, "strlastindexof", ts -> INT32));

	@Test
	public void toStringAndResultTypeMatchEachOperator() {
		for (Case c : CASES) {
			assertEquals(c.symbol, c.operator.toString());
			Set<Type> strs = Collections.singleton(STR);
			assertEquals(Collections.singleton(c.resultType.apply(TS)), c.operator.typeInference(TS, strs, strs),
					c.symbol);
		}
	}

	@Test
	public void typeInferenceRequiresStringOnBothSidesForEveryOperator() {
		Set<Type> strs = Collections.singleton(STR);
		Set<Type> other = Collections.singleton(INT32);
		for (Case c : CASES) {
			assertTrue(c.operator.typeInference(TS, strs, other).isEmpty(), c.symbol + ": right operand not a string");
			assertTrue(c.operator.typeInference(TS, other, strs).isEmpty(), c.symbol + ": left operand not a string");
		}
	}

}
