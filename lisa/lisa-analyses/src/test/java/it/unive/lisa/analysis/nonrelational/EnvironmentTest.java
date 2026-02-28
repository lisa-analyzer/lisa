package it.unive.lisa.analysis.nonrelational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class EnvironmentTest {

	// we use a value environment as candidate since it is the simpler
	// implementation possible
	private static final ValueEnvironment<SignLattice> env = new ValueEnvironment<>(SignLattice.BOTTOM);

	private static final Identifier varA = new Variable(Untyped.INSTANCE, "a", SyntheticLocation.INSTANCE);

	private static final Identifier varB = new Variable(Untyped.INSTANCE, "b", SyntheticLocation.INSTANCE);

	private static final Identifier heapA = new HeapLocation(Untyped.INSTANCE, "a", false, SyntheticLocation.INSTANCE);

	private static final Identifier heapB = new HeapLocation(Untyped.INSTANCE, "b", false, SyntheticLocation.INSTANCE);

	private static final Identifier heapAweak = new HeapLocation(
			Untyped.INSTANCE,
			"a",
			true,
			SyntheticLocation.INSTANCE);

	private static final Identifier heapBweak = new HeapLocation(
			Untyped.INSTANCE,
			"b",
			true,
			SyntheticLocation.INSTANCE);

	private final ProgramPoint pp = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}

	};

	@Test
	public void testLubKeys()
			throws SemanticException {
		assertEquals(Set.of(varA), env.lubKeys(Set.of(varA), Set.of(varA)));
		assertEquals(Set.of(varA, varB), env.lubKeys(Set.of(varA), Set.of(varB)));
		assertEquals(Set.of(heapAweak), env.lubKeys(Set.of(heapAweak), Set.of(heapA)));
		assertEquals(Set.of(heapAweak, heapBweak), env.lubKeys(Set.of(heapAweak), Set.of(heapBweak)));
		assertEquals(Set.of(heapA, heapB), env.lubKeys(Set.of(heapA), Set.of(heapB)));
	}

	@Test
	public void testForgetIdentifier()
			throws SemanticException {
		ValueEnvironment<SignLattice> tmp = env.top();
		assertSame(tmp, tmp.forgetIdentifier(varA, pp));
		tmp = env.bottom();
		assertSame(tmp, tmp.forgetIdentifier(varA, pp));
		tmp = env.putState(varA, SignLattice.TOP);
		assertEquals(env, tmp.forgetIdentifier(varA, pp));
		assertEquals(tmp, tmp.forgetIdentifier(varB, pp));
	}

	@Test
	public void testScopes()
			throws SemanticException {
		SignLattice state = SignLattice.TOP;
		ScopeToken scoper = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}

		});
		ValueEnvironment<SignLattice> tmp = env.top();
		ValueEnvironment<SignLattice> onlyA = tmp.putState(varA, state);

		ValueEnvironment<SignLattice> onlyAscoped = tmp.putState((Identifier) varA.pushScope(scoper, pp), state);
		ValueEnvironment<SignLattice> actual = onlyA.pushScope(scoper, pp);
		assertEquals(onlyAscoped, actual);
		assertEquals(onlyA, actual.popScope(scoper, pp));

		ValueEnvironment<SignLattice> AandB = onlyA.putState(heapB, state);
		ValueEnvironment<
				SignLattice> AandBscoped = onlyAscoped.putState((Identifier) heapB.pushScope(scoper, pp), state);
		assertEquals(AandBscoped, AandB.pushScope(scoper, pp));
	}

}
