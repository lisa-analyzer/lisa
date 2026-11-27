package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the runs of {@link FreshValueFactory}: within the replay of a node,
 * the same unknown value is always replaced by the same fresh value.
 */
public class FreshValueFactoryTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 1, 1);

	@Test
	public void unknownValuesAreFreshOncePerRun() {
		FreshValueFactory factory = new FreshValueFactory(new InputSourceResolver(List.of(), null));
		PushAny value = new PushAny(Int32Type.INSTANCE, LOC);
		PushAny other = new PushAny(Int32Type.INSTANCE, LOC);

		// outside runs, every replacement is a new value
		assertNotEquals(factory.fresh(value, null), factory.fresh(value, null));

		factory.startRun();
		FreshValue first = factory.fresh(value, null);
		// the same evaluation is the same value
		assertSame(first, factory.fresh(value, null));
		// another evaluation (even if equal) is another value
		assertNotEquals(first, factory.fresh(other, null));

		// nested runs have their own values
		factory.startRun();
		assertNotEquals(first, factory.fresh(value, null));
		factory.endRun();
		assertSame(first, factory.fresh(value, null));
		factory.endRun();

		// a new run yields new values
		factory.startRun();
		assertNotEquals(first, factory.fresh(value, null));
		factory.endRun();
	}
}
