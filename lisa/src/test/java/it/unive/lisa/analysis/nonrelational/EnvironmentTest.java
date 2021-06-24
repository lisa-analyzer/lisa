package it.unive.lisa.analysis.nonrelational;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Set;

import org.junit.Test;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;

public class EnvironmentTest {

	// we use a value environment as candidate since it is the simpler
	// implementation possible
	private static final ValueEnvironment<Sign> env = new ValueEnvironment<>(new Sign().bottom());

	private static final Identifier varA = new Variable(null, "a", SyntheticLocation.INSTANCE);
	private static final Identifier varB = new Variable(null, "b", SyntheticLocation.INSTANCE);
	private static final Identifier heapA = new HeapLocation(null, "a", false, SyntheticLocation.INSTANCE);
	private static final Identifier heapB = new HeapLocation(null, "b", false, SyntheticLocation.INSTANCE);
	private static final Identifier heapAweak = new HeapLocation(null, "a", true, SyntheticLocation.INSTANCE);
	private static final Identifier heapBweak = new HeapLocation(null, "b", true, SyntheticLocation.INSTANCE);

	@Test
	public void testLubKeys() throws SemanticException {
		assertEquals(Set.of(varA), env.lubKeys(Set.of(varA), Set.of(varA)));
		assertEquals(Set.of(varA, varB), env.lubKeys(Set.of(varA), Set.of(varB)));
		assertEquals(Set.of(heapAweak), env.lubKeys(Set.of(heapAweak), Set.of(heapA)));
		assertEquals(Set.of(heapAweak, heapBweak), env.lubKeys(Set.of(heapAweak), Set.of(heapBweak)));
		assertEquals(Set.of(heapA, heapB), env.lubKeys(Set.of(heapA), Set.of(heapB)));
	}

	@Test
	public void testForgetIdentifier() throws SemanticException {
		ValueEnvironment<Sign> tmp = env.top();
		assertSame(tmp, tmp.forgetIdentifier(varA));
		tmp = env.bottom();
		assertSame(tmp, tmp.forgetIdentifier(varA));
		tmp = env.putState(varA, new Sign());
		assertEquals(env, tmp.forgetIdentifier(varA));
		assertEquals(tmp, tmp.forgetIdentifier(varB));
	}

	@Test
	public void testScopes() throws SemanticException {
		Sign state = new Sign();
		ScopeToken scoper = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}
		});
		ValueEnvironment<Sign> tmp = env.top();
		ValueEnvironment<Sign> onlyA = tmp.putState(varA, state);

		ValueEnvironment<Sign> onlyAscoped = tmp.putState((Identifier) varA.pushScope(scoper), state);
		ValueEnvironment<Sign> actual = onlyA.pushScope(scoper);
		assertEquals(onlyAscoped, actual);
		assertEquals(onlyA, actual.popScope(scoper));
		
		
		// TODO: this currently fails due to the errors in push/pop of environment and identifiers
		// enable and extend when we merge the branch about heap references
//		ValueEnvironment<Sign> AandB = onlyA.putState(heapB, state);
//		ValueEnvironment<Sign> AandBscoped = onlyAscoped.putState((Identifier) heapB.pushScope(scoper), state);
//		assertEquals(AandBscoped, AandB.pushScope(scoper));
	}
}
