package it.unive.lisa.analysis.numeric;

import java.util.Objects;
import java.util.function.Predicate;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * Implementation of the octagons analysis of
 * <a href="https://arxiv.org/pdf/cs/0703084">this paper</a>.
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *         Shytermeja</a>
 */
public class Octagon
		implements
		ValueDomain<Octagon>,
		BaseLattice<Octagon> {

	private final DifferenceBoundMatrix dbm;

	/**
	 * Builds a new octagon instance.
	 */
	public Octagon() {
		this(new DifferenceBoundMatrix());
	}

	Octagon(DifferenceBoundMatrix dbm) {
		this.dbm = dbm;
	}

	private void debug(String message) {
		// System.out.println("Octagon: " + message);
	}

	@Override
	public Octagon assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		debug("assign() - Assigning " + expression + " to " + id);
		return new Octagon(dbm.assign(id, expression, pp, oracle));
	}

	@Override
	public Octagon forgetIdentifier(Identifier id) throws SemanticException {
		debug("forgetIdentifier() - Forgetting " + id);
		return new Octagon(dbm.forgetIdentifier(id));
	}

	@Override
	public Octagon forgetIdentifiers(Iterable<Identifier> ids) throws SemanticException {
		debug("forgetIdentifiers() - Forgetting identifiers: " + ids);
		return ValueDomain.super.forgetIdentifiers(ids);
	}

	@Override
	public StructuredRepresentation representation() {
		debug("representation() - Getting representation");
		return dbm.representation();
	}

	@Override
	public Octagon glbAux(Octagon other) throws SemanticException {
		debug("glbAux() - Computing glb with " + other);
		return BaseLattice.super.glbAux(other);
	}

	@Override
	public boolean lessOrEqualAux(Octagon other) throws SemanticException {
		debug("lessOrEqualAux() - Checking less or equal with " + other);
		return dbm.lessOrEqualAux(other.dbm);
	}

	@Override
	public Octagon lubAux(Octagon other) throws SemanticException {
		debug("lubAux() - Computing lub with " + other);
		return new Octagon(dbm.lubAux(other.dbm));
	}

	@Override
	public Octagon narrowingAux(Octagon other) throws SemanticException {
		debug("narrowingAux() - Computing narrowing with " + other);
		return BaseLattice.super.narrowingAux(other);
	}

	@Override
	public Octagon wideningAux(Octagon other) throws SemanticException {
		debug("wideningAux() - Computing widening with " + other);
		return new Octagon(dbm.wideningAux(other.dbm));
	}

	@Override
	public Octagon smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		debug("smallStepSemantics() - Computing small step semantics of " + expression);
		return new Octagon(dbm.smallStepSemantics(expression, pp, oracle));
	}

	@Override
	public Octagon assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
			throws SemanticException {
		debug("assume() - Assuming " + expression + " from " + src + " to " + dest);
		return new Octagon(dbm.assume(expression, src, dest, oracle));
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		debug("knowsIdentifier() - Checking if knows " + id);
		return dbm.knowsIdentifier(id);
	}

	@Override
	public Octagon forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		debug("forgetIdentifiersIf() - Forgetting identifiers that satisfy a test");
		return new Octagon(dbm.forgetIdentifiersIf(test));
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		debug("satisfies() - Checking if satisfies " + expression);
		return dbm.satisfies(expression, pp, oracle);
	}

	@Override
	public Octagon pushScope(ScopeToken token) throws SemanticException {
		debug("pushScope() - Pushing scope " + token);
		return new Octagon(dbm.pushScope(token));
	}

	@Override
	public Octagon popScope(ScopeToken token) throws SemanticException {
		debug("popScope() - Popping scope " + token);
		return new Octagon(dbm.popScope(token));
	}

	@Override
	public boolean lessOrEqual(Octagon other) throws SemanticException {
		debug("lessOrEqual() - Checking less or equal with " + other);
		return BaseLattice.super.lessOrEqual(other);
	}

	@Override
	public Octagon lub(Octagon other) throws SemanticException {
		debug("lub() - Computing lub with " + other);
		return BaseLattice.super.lub(other);
	}

	@Override
	public Octagon top() {
		debug("top() - Getting top element");
		return new Octagon(new DifferenceBoundMatrix().top());
	}

	@Override
	public Octagon bottom() {
		debug("bottom() - Getting bottom element");
		return new Octagon(new DifferenceBoundMatrix().bottom());
	}

	@Override
	public boolean isTop() {
		debug("isTop() - Checking if is top");
		return dbm.isTop();
	}

	@Override
	public boolean isBottom() {
		debug("isBottom() - Checking if is bottom");
		return dbm.isBottom();
	}

	public static Octagon fromIntervalDomain(ValueEnvironment<Interval> env) throws SemanticException {
		return new Octagon(DifferenceBoundMatrix.fromIntervalDomain(env));
	}

	public ValueEnvironment<Interval> toIntervalDomain() throws SemanticException {
		return dbm.toInterval();
	}

	@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    
    Octagon that = (Octagon) obj;
    return Objects.equals(this.dbm, that.dbm);
}

@Override
public int hashCode() {
    return Objects.hash(dbm);
}

}