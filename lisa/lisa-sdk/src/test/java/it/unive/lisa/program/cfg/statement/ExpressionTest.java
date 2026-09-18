package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

	@Test
	public void aStandaloneExpressionIsItsOwnRootStatementAndHasNoParent() {
		VariableRef v = new VariableRef(newCfg(), LOC, "x");
		assertSame(v, v.getRootStatement());
		assertNull(v.getParentStatement());
	}

	@Test
	public void getRootStatementWalksUpThroughNestedExpressionsButStopsAtANonExpressionParent() {
		CFG cfg = newCfg();
		VariableRef target = new VariableRef(cfg, LOC, "x");
		VariableRef value = new VariableRef(cfg, LOC, "a");
		Assignment assignment = new Assignment(cfg, LOC, target, value);
		Return outer = new Return(cfg, LOC, assignment);

		// target/value are nested inside the assignment expression: their root
		// is the outer statement, not the assignment itself
		assertSame(outer, target.getRootStatement());
		assertSame(outer, value.getRootStatement());
		// the assignment's parent is a Statement that is not an Expression, so
		// it is returned as-is instead of being walked further
		assertSame(outer, assignment.getRootStatement());
		assertSame(outer, assignment.getParentStatement());
		assertSame(assignment, target.getParentStatement());
	}

	@Test
	public void setParentStatementIsIgnoredOnceAParentIsAlreadySet() {
		CFG cfg = newCfg();
		VariableRef v = new VariableRef(cfg, LOC, "x");
		NoOp first = new NoOp(cfg, LOC);
		NoOp second = new NoOp(cfg, LOC);

		v.setParentStatement(first);
		assertSame(first, v.getParentStatement());

		v.setParentStatement(second);
		assertSame(first, v.getParentStatement());
	}

	@Test
	public void metaVariablesStartsEmptyAndIsDirectlyMutable() {
		VariableRef v = new VariableRef(newCfg(), LOC, "x");
		assertTrue(v.getMetaVariables().isEmpty());

		Identifier id = new Variable(Untyped.INSTANCE, "meta", LOC);
		v.getMetaVariables().add(id);
		assertTrue(v.getMetaVariables().contains(id));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnLocationAndStaticType() {
		CFG cfg = newCfg();
		DefaultParamInitialization a = new DefaultParamInitialization(cfg, LOC, Untyped.INSTANCE);
		DefaultParamInitialization b = new DefaultParamInitialization(cfg, LOC, Untyped.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		DefaultParamInitialization differentType = new DefaultParamInitialization(cfg, LOC, VoidType.INSTANCE);
		assertNotEquals(a, differentType);
		assertFalse(a.equals(null));
	}

}
