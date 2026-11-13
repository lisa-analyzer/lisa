package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import org.junit.jupiter.api.Test;

public class NativeCFGTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	/**
	 * A minimal {@link PluggableStatement} construct usable by
	 * {@link NativeCFG}.
	 */
	public static class FakeConstruct
			extends
			NaryExpression
			implements
			PluggableStatement {

		private Statement originating;

		public static FakeConstruct build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new FakeConstruct(cfg, location, params);
		}

		public FakeConstruct(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			super(cfg, location, "fake", params);
		}

		@Override
		public void setOriginatingStatement(
				Statement st) {
			this.originating = st;
		}

		public Statement getOriginatingStatement() {
			return originating;
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				ExpressionSet[] params,
				StatementStore<A> expressions)
				throws SemanticException {
			return state;
		}

	}

	/** A statement that does NOT implement {@link PluggableStatement}. */
	public static class NotPluggable
			extends
			NaryExpression {

		public NotPluggable(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			super(cfg, location, "notPluggable", params);
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				ExpressionSet[] params,
				StatementStore<A> expressions)
				throws SemanticException {
			return state;
		}

	}

	@Test
	public void constructorRejectsAConstructNotImplementingPluggableStatement() {
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, cfg().getDescriptor().getUnit(), false, "n");
		assertThrows(IllegalArgumentException.class, () -> new NativeCFG(descriptor, NotPluggable.class));
	}

	@Test
	public void rewriteBuildsAnInstanceAndLinksItToTheOriginatingStatement() throws CallResolutionException {
		CFG cfg = cfg();
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, cfg.getDescriptor().getUnit(), false, "n");
		NativeCFG native_ = new NativeCFG(descriptor, FakeConstruct.class);

		Statement original = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		Expression param = new VariableRef(cfg, LOC, "a");

		NaryExpression rewritten = native_.rewrite(original, param);

		assertEquals(FakeConstruct.class, rewritten.getClass());
		assertSame(original, ((FakeConstruct) rewritten).getOriginatingStatement());
		assertEquals(1, rewritten.getSubExpressions().length);
		assertSame(param, rewritten.getSubExpressions()[0]);
	}

	@Test
	public void getDescriptorAndToStringDelegateToTheDescriptor() {
		CFG cfg = cfg();
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, cfg.getDescriptor().getUnit(), false, "n");
		NativeCFG native_ = new NativeCFG(descriptor, FakeConstruct.class);

		assertSame(descriptor, native_.getDescriptor());
		assertEquals(descriptor.toString(), native_.toString());
	}

}
