package it.unive.lisa.interprocedural.callgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RTACallGraphTest {

	private static Program program;
	private static CFG cfg;
	private static ClassType base;
	private static ClassType sub;
	private static ClassType unrelated;

	@BeforeAll
	public static void init()
			throws ParsingException {
		program = IMPFrontend.processText(
				"class Base { } class Sub extends Base { } class Unrelated { } class C { foo() { } }");
		cfg = program.getAllCFGs().iterator().next();
		base = ClassType.lookup("Base");
		sub = ClassType.lookup("Sub");
		unrelated = ClassType.lookup("Unrelated");
	}

	private Expression receiverOfType(
			ClassType type) {
		return new VariableRef(cfg, SyntheticLocation.INSTANCE, "recv", type);
	}

	@Test
	public void resolvesExactlyToTheGivenInstantiatedTypes() {
		RTACallGraph rta = new RTACallGraph();
		Expression receiver = receiverOfType(base);

		Set<Type> given = Set.of(sub);
		Set<Type> result = new HashSet<>(rta.getPossibleTypesOfReceiver(receiver, given));

		assertEquals(given, result);
	}

	@Test
	public void doesNotExpandToTheFullClassHierarchyLikeCHADoes() {
		RTACallGraph rta = new RTACallGraph();
		CHACallGraph cha = new CHACallGraph();
		Expression receiver = receiverOfType(base);

		// only Sub is known to be instantiated: RTA must resolve to exactly
		// that, not to every subtype of Base the way CHA would
		Set<Type> given = Set.of(sub);
		Set<Type> rtaResult = new HashSet<>(rta.getPossibleTypesOfReceiver(receiver, given));
		Set<Type> chaResult = new HashSet<>(cha.getPossibleTypesOfReceiver(receiver, given));

		assertEquals(given, rtaResult);
		assertNotEquals(chaResult, rtaResult);
	}

	@Test
	public void anEmptyInstantiatedSetResolvesToNothing() {
		RTACallGraph rta = new RTACallGraph();
		Expression receiver = receiverOfType(base);

		Set<Type> result = new HashSet<>(rta.getPossibleTypesOfReceiver(receiver, Set.of()));

		assertEquals(Set.of(), result);
	}

}
