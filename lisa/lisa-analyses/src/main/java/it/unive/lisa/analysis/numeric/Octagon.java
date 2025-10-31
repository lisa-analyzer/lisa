package it.unive.lisa.analysis.numeric;

import java.util.Map;
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
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The octagon abstract domain for relational numerical analysis, implementing
 * constraints of the form ±x ± y ≤ c where x and y are program variables and c
 * is a constant. This domain is more precise than intervals as it can express
 * relationships between variables, while being more efficient than full
 * polyhedra. It is implemented as a {@link ValueDomain}, handling top and
 * bottom values through its underlying {@link DifferenceBoundMatrix}
 * representation. Top and bottom cases for lattice operations are handled by
 * {@link BaseLattice}.
 * <p>
 * The implementation is based on the octagon abstract domain described in
 * <a href="https://arxiv.org/pdf/cs/0703084">Miné's paper on the Octagon
 * Abstract Domain</a>.
 * </p>
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso</a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */
public class Octagon
		implements
		ValueDomain<Octagon>,
		BaseLattice<Octagon> {

	/**
	 * The underlying difference-bound matrix representation of the octagon
	 * constraints.
	 */
	private final DifferenceBoundMatrix dbm;

	/**
	 * Builds a new empty octagon instance (top element).
	 */
	public Octagon() {
		this(new DifferenceBoundMatrix());
	}

	/**
	 * Builds an octagon from the given difference-bound matrix.
	 * 
	 * @param dbm the underlying {@link DifferenceBoundMatrix}
	 */
	Octagon(
			DifferenceBoundMatrix dbm) {
		this.dbm = dbm;
	}

	private void debug(
			String message) {
		// System.out.println("Octagon: " + message);
	}

	/**
	 * Performs an assignment operation in the octagon domain.
	 * 
	 * @see DifferenceBoundMatrix#assign(Identifier, ValueExpression,
	 *          ProgramPoint, SemanticOracle)
	 */
	@Override
	public Octagon assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		debug("assign() - Assigning " + expression + " to " + id);

		// Esegui l'assegnamento
		DifferenceBoundMatrix newDBM = dbm.assign(id, expression, pp, oracle);

		return new Octagon(newDBM);
	}

	/**
	 * Forgets (removes constraints on) the specified identifier.
	 * 
	 * @see DifferenceBoundMatrix#forgetIdentifier(Identifier)
	 */
	@Override
	public Octagon forgetIdentifier(
			Identifier id)
			throws SemanticException {
		debug("forgetIdentifier() - Forgetting " + id);
		return new Octagon(dbm.forgetIdentifier(id));
	}

	@Override
	public Octagon forgetIdentifiers(
			Iterable<Identifier> ids)
			throws SemanticException {
		debug("forgetIdentifiers() - Forgetting identifiers: " + ids);
		return ValueDomain.super.forgetIdentifiers(ids);
	}

	/**
	 * Returns a structured representation of the octagon constraints as a set
	 * of linear inequalities of the form ±x ± y ≤ c.
	 */
	@Override
	public StructuredRepresentation representation() {
		debug("representation() - Getting representation");
		Map<Identifier, Integer> variableIndex = dbm.getVariableIndex();
		MathNumber[][] matrix = dbm.getMatrix();

		// Use a set to automatically handle duplicates
		java.util.Set<String> constraints = new java.util.LinkedHashSet<>();

		// Single variable constraints (±x ± x ≤ c)
		for (Identifier id : variableIndex.keySet()) {
			int posIdx = dbm.idToPos(id, variableIndex);
			int negIdx = dbm.idToNeg(id, variableIndex);

			// x - (-x) ≤ c
			if (posIdx < matrix.length && negIdx < matrix.length) {
				constraints.add("{" + id.getName() + " -(-" + id.getName() + ") <= "
						+ matrix[negIdx][posIdx] + "}");
			}

			// -x - x ≤ c
			if (negIdx < matrix.length && posIdx < matrix.length) {
				constraints.add("{-" + id.getName() + " -" + id.getName() + " <= "
						+ matrix[posIdx][negIdx] + "}");
			}
		}

		// Two variable constraints (±x ± y ≤ c)
		for (Identifier id1 : variableIndex.keySet()) {
			int pos1 = dbm.idToPos(id1, variableIndex);
			int neg1 = dbm.idToNeg(id1, variableIndex);

			for (Identifier id2 : variableIndex.keySet()) {
				if (id1.getName().equals(id2.getName())) {
					continue;
				}

				int pos2 = dbm.idToPos(id2, variableIndex);
				int neg2 = dbm.idToNeg(id2, variableIndex);

				if (pos1 >= matrix.length || neg1 >= matrix.length ||
						pos2 >= matrix.length || neg2 >= matrix.length) {
					continue;
				}

				// x - y ≤ c
				constraints.add("{" + id1.getName() + " - " + id2.getName() + " <= "
						+ matrix[pos2][pos1] + "}");

				// -x - (-y) ≤ c
				constraints.add("{-" + id1.getName() + " -(-" + id2.getName() + ") <= "
						+ matrix[pos2][neg1] + "}");

				// x - (-y) ≤ c
				constraints.add("{" + id1.getName() + " -(-" + id2.getName() + ") <= "
						+ matrix[neg2][pos1] + "}");

				// -x - y ≤ c
				constraints.add("{-" + id1.getName() + " - " + id2.getName() + " <= "
						+ matrix[pos2][neg1] + "}");
			}
		}

		// Build final result
		StringBuilder result = new StringBuilder("<br>");
		for (String constraint : constraints) {
			result.append(constraint).append("<br>");
		}

		return new StringRepresentation(result.toString());
	}

	/**
	 * Computes the greatest lower bound (meet) of two octagons.
	 * 
	 * @see DifferenceBoundMatrix#glbAux(DifferenceBoundMatrix)
	 */
	@Override
	public Octagon glbAux(
			Octagon other)
			throws SemanticException {
		 debug("glbAux() - Computing glb with " + other);
    
    // ELIMINA questa linea che causa ricorsione:
    // return BaseLattice.super.glbAux(other);
    
    // Sostituisci con:
    DifferenceBoundMatrix newDBM;
    
    synchronized (this.dbm) {
        synchronized (other.dbm) {
            newDBM = this.dbm.glbAux(other.dbm);
        }
    }
    
    return new Octagon(newDBM);
	}

	/**
	 * Checks the partial order relation between two octagons.
	 * 
	 * @see DifferenceBoundMatrix#lessOrEqualAux(DifferenceBoundMatrix)
	 */
	@Override
	public boolean lessOrEqualAux(
			Octagon other)
			throws SemanticException {
		debug("lessOrEqualAux() - Checking less or equal with " + other);
		
		 synchronized (this.dbm) {
        synchronized (other.dbm) {
            return dbm.lessOrEqualAux(other.dbm);
        }
    }
	}

	/**
	 * Computes the least upper bound (join) of two octagons.
	 * 
	 * @see DifferenceBoundMatrix#lubAux(DifferenceBoundMatrix)
	 */
	@Override
	public Octagon lubAux(
			Octagon other)
			throws SemanticException {
		
				 // CREA NUOVI DBM invece di usare quelli esistenti
     DifferenceBoundMatrix newDBM;
    
    synchronized (this.dbm) {
        synchronized (other.dbm) {
            newDBM = this.dbm.lubAux(other.dbm);
        }
    }
    
    return new Octagon(newDBM);
	}

	/**
	 * Computes the narrowing of two octagons to improve precision after
	 * widening.
	 */
	@Override
	public Octagon narrowingAux(
			Octagon other)
			throws SemanticException {
		debug("narrowingAux() - Computing narrowing with " + other);
		return BaseLattice.super.narrowingAux(other);
	}

	/**
	 * Computes the widening of two octagons to ensure termination.
	 * 
	 * @see DifferenceBoundMatrix#wideningAux(DifferenceBoundMatrix)
	 */
	@Override
	public Octagon wideningAux(
			Octagon other)
			throws SemanticException {
		debug("wideningAux() - Computing widening with " + other);
		 DifferenceBoundMatrix newDBM;
    
    synchronized (this.dbm) {
        synchronized (other.dbm) {
            newDBM = dbm.wideningAux(other.dbm);
        }
    }
    
    return new Octagon(newDBM);
	}

	/**
	 * Evaluates the small-step semantics of an expression in the octagon
	 * domain.
	 * 
	 * @see DifferenceBoundMatrix#smallStepSemantics(ValueExpression,
	 *          ProgramPoint, SemanticOracle)
	 */
	@Override
	public Octagon smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		debug("smallStepSemantics() - Computing small step semantics of " + expression);
		return new Octagon(dbm.smallStepSemantics(expression, pp, oracle));
	}

	/**
	 * Refines the octagon by assuming a boolean constraint holds.
	 * 
	 * @see DifferenceBoundMatrix#assume(ValueExpression, ProgramPoint,
	 *          ProgramPoint, SemanticOracle)
	 */
	@Override
	public Octagon assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		debug("assume() - Assuming " + expression + " from " + src + " to " + dest);
		return new Octagon(dbm.assume(expression, src, dest, oracle));
	}

	/**
	 * Checks whether the octagon tracks the given identifier.
	 * 
	 * @see DifferenceBoundMatrix#knowsIdentifier(Identifier)
	 */
	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		debug("knowsIdentifier() - Checking if knows " + id);
		return dbm.knowsIdentifier(id);
	}

	/**
	 * Forgets all identifiers that satisfy the given predicate.
	 * 
	 * @see DifferenceBoundMatrix#forgetIdentifiersIf(Predicate)
	 */
	@Override
	public Octagon forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		debug("forgetIdentifiersIf() - Forgetting identifiers that satisfy a test");
		return new Octagon(dbm.forgetIdentifiersIf(test));
	}

	/**
	 * Checks whether the given expression is satisfied by this octagon.
	 * 
	 * @see DifferenceBoundMatrix#satisfies(ValueExpression, ProgramPoint,
	 *          SemanticOracle)
	 */
	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		debug("satisfies() - Checking if satisfies " + expression);
		return dbm.satisfies(expression, pp, oracle);
	}

	/**
	 * Pushes a new scope for scoped identifiers.
	 * 
	 * @see DifferenceBoundMatrix#pushScope(ScopeToken)
	 */
	@Override
	public Octagon pushScope(
			ScopeToken token)
			throws SemanticException {
		debug("pushScope() - Pushing scope " + token);
		return new Octagon(dbm.pushScope(token));
	}

	/**
	 * Pops a scope, removing scoped identifiers.
	 * 
	 * @see DifferenceBoundMatrix#popScope(ScopeToken)
	 */
	@Override
	public Octagon popScope(
			ScopeToken token)
			throws SemanticException {
		debug("popScope() - Popping scope " + token);
		return new Octagon(dbm.popScope(token));
	}

	/**
	 * Checks the partial order relation between two octagons.
	 */
	@Override
	public boolean lessOrEqual(
			Octagon other)
			throws SemanticException {
		
				 // Non chiamare super.lessOrEqual() che potrebbe causare ricorsione
    synchronized (this.dbm) {
        synchronized (other.dbm) {
            return this.dbm.lessOrEqualAux(other.dbm);
        }
    }
	}

	/**
	 * Computes the least upper bound (join) of two octagons.
	 */
	@Override
	public Octagon lub(
			Octagon other)
			throws SemanticException {
		debug("lub() - Computing lub with " + other);
		
		 
    // Non chiamare super.lub() che potrebbe causare ricorsione
    synchronized (this.dbm) {
        synchronized (other.dbm) {
            DifferenceBoundMatrix newDBM = this.dbm.lubAux(other.dbm);
            return new Octagon(newDBM);
        }
    }
	}

	/**
	 * Returns the top element (no constraints) of the octagon domain.
	 * 
	 * @see DifferenceBoundMatrix#top()
	 */
	@Override
	public Octagon top() {
		debug("top() - Getting top element");
		return new Octagon(new DifferenceBoundMatrix().top());
	}

	/**
	 * Returns the bottom element (inconsistent constraints) of the octagon
	 * domain.
	 * 
	 * @see DifferenceBoundMatrix#bottom()
	 */
	@Override
	public Octagon bottom() {
		debug("bottom() - Getting bottom element");
		return new Octagon(new DifferenceBoundMatrix().bottom());
	}

	/**
	 * Checks whether this octagon represents the top element.
	 * 
	 * @see DifferenceBoundMatrix#isTop()
	 */
	@Override
	public boolean isTop() {
		debug("isTop() - Checking if is top");
		return dbm.isTop();
	}

	/**
	 * Checks whether this octagon represents the bottom element.
	 * 
	 * @see DifferenceBoundMatrix#isBottom()
	 */
	@Override
	public boolean isBottom() {
		debug("isBottom() - Checking if is bottom");
		return dbm.isBottom();
	}

	/**
	 * Converts an interval domain environment to an octagon domain. This allows
	 * lifting non-relational interval constraints to the relational octagon
	 * domain.
	 * 
	 * @param env the interval environment to convert
	 * 
	 * @return the octagon representation of the interval constraints
	 * 
	 * @throws SemanticException if the conversion fails
	 * 
	 * @see DifferenceBoundMatrix#fromIntervalDomain(ValueEnvironment)
	 */
	public static Octagon fromIntervalDomain(
			ValueEnvironment<Interval> env)
			throws SemanticException {
		return new Octagon(DifferenceBoundMatrix.fromIntervalDomain(env));
	}

	/**
	 * Converts this octagon to an interval domain environment. This projects
	 * the relational octagon constraints to non-relational interval constraints
	 * for each variable.
	 * 
	 * @return the interval environment representation
	 * 
	 * @throws SemanticException if the conversion fails
	 * 
	 * @see DifferenceBoundMatrix#toInterval()
	 */
	public ValueEnvironment<Interval> toIntervalDomain() throws SemanticException {
		return dbm.toInterval();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		Octagon that = (Octagon) obj;
		return Objects.equals(this.dbm, that.dbm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dbm);
	}

}