package it.unive.lisa.interprocedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import org.junit.jupiter.api.Test;

public class UniqueScopeTest {

	@Test
	public void aFreshScopeIsAlwaysTheStartingOne() {
		UniqueScope<TestAbstractState> scope = new UniqueScope<>();
		assertTrue(scope.isStartingId());
		assertSame(scope, scope.startingId());
	}

	@Test
	public void pushingACallDoesNotCreateANewScope() {
		// UniqueScope does not distinguish calling contexts: pushing any call
		// (or even no call at all) must always yield the very same id
		UniqueScope<TestAbstractState> scope = new UniqueScope<>();
		assertSame(scope, scope.push(null, null));
	}

}
