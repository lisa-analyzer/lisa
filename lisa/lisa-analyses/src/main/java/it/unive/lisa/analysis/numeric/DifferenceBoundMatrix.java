package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.octagon.BooleanExpressionNormalizer;
import it.unive.lisa.util.octagon.Floyd;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.lang.model.util.ElementScanner14;

/**
 * The Difference Bound Matrix (DBM) abstract domain for representing octagon
 * constraints. A DBM is a square matrix of size 2n × 2n for n program
 * variables, where each entry m[i][j] represents the upper bound constraint V_i
 * - V_j ≤ m[i][j]. The matrix provides an efficient representation for octagon
 * constraints of the form ±x ± y ≤ c, which are fundamental for relational
 * numerical analysis. It is implemented as a {@link ValueDomain}, handling top
 * and bottom values for lattice operations. Top and bottom cases are managed by
 * {@link BaseLattice}.
 * <p>
 * The implementation follows the octagon abstract domain described in
 * <a href="https://arxiv.org/pdf/cs/0703084">Miné's paper on the Octagon
 * Abstract Domain</a>, using difference-bound matrices as the underlying
 * representation with strong closure computation via the Floyd-Warshall
 * algorithm.
 * </p>
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso</a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */

public class DifferenceBoundMatrix
		implements
		BaseLattice<DifferenceBoundMatrix>,
		ValueDomain<DifferenceBoundMatrix> {

	/**
	 * The underlying matrix representation of size 2n × 2n for n variables.
	 * Each entry matrix[i][j] represents the constraint V_i - V_j ≤
	 * matrix[i][j].
	 */
	private final MathNumber[][] matrix;

	/**
	 * Mapping from program identifiers to their corresponding indices in the
	 * matrix. Each variable x is associated with two indices: 2k (for x) and
	 * 2k+1 (for -x).
	 */
	private final Map<Identifier, Integer> variableIndex;

	/**
	 * Builds an empty difference-bound matrix with no variables.
	 */
	public DifferenceBoundMatrix() {
		this.matrix = new MathNumber[0][0];
		this.variableIndex = new java.util.HashMap<Identifier, Integer>();

	}

	/**
	 * Builds a difference-bound matrix from the given matrix and variable index
	 * mapping.
	 * 
	 * @param matrix        the 2n × 2n matrix of constraints
	 * @param variableIndex the mapping from identifiers to matrix indices
	 */
	public DifferenceBoundMatrix(
			MathNumber[][] matrix,
			Map<Identifier, Integer> variableIndex) {
		this.matrix = matrix;
		this.variableIndex = variableIndex;
	}

	/**
	 * Returns the variable index mapping.
	 * 
	 * @return the map from identifiers to matrix indices
	 */
	public Map<Identifier, Integer> getVariableIndex() {
		return variableIndex;
	}

	/**
	 * Returns the underlying constraint matrix.
	 * 
	 * @return the 2n × 2n matrix of difference bounds
	 */
	public MathNumber[][] getMatrix() {
		return matrix;
	}

	/**
	 * Creates a deep copy of the given matrix.
	 * 
	 * @param source the matrix to copy
	 * 
	 * @return a new matrix with the same values
	 */
	private MathNumber[][] copyMatrix(
			MathNumber[][] source) {
		MathNumber[][] copy = new MathNumber[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = new MathNumber[source[i].length];
			System.arraycopy(source[i], 0, copy[i], 0, source[i].length);
		}
		return copy;
	}

	@Override
	public DifferenceBoundMatrix top() {
		int size = 1;
		if (matrix != null && matrix.length > 0) {
			size = matrix.length;
		}

		MathNumber[][] topMatrix = new MathNumber[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				topMatrix[i][j] = MathNumber.PLUS_INFINITY;
			}
		}

		Map<Identifier, Integer> newIndex = new HashMap<>();
		if (variableIndex != null) {
			newIndex.putAll(variableIndex);
		}

		return new DifferenceBoundMatrix(topMatrix, newIndex);
	}

	@Override
	public DifferenceBoundMatrix bottom() {

		int size = 1;
		if (matrix != null && matrix.length > 0) {
			size = matrix.length;
		}

		MathNumber[][] bottomMatrix = new MathNumber[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (i == j) {
					bottomMatrix[i][j] = new MathNumber(-1);
				} else {
					bottomMatrix[i][j] = MathNumber.PLUS_INFINITY;
				}
			}
		}

		Map<Identifier, Integer> newIndex = new HashMap<>();
		if (variableIndex != null) {
			newIndex.putAll(variableIndex);
		}

		return new DifferenceBoundMatrix(bottomMatrix, newIndex);
	}

	@Override
	public boolean isTop() {
		if (matrix == null)
			return false;

		if (matrix.length == 0)
			return false;

		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i] == null)
				return false;
			for (int j = 0; j < matrix[i].length; j++) {
				MathNumber element = matrix[i][j];
				if (element == null || !element.equals(MathNumber.PLUS_INFINITY)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean isBottom() {
		if (matrix == null)
			return false;

		if (matrix.length == 0)
			return true;

		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i] == null)
				return false;
			if (i < matrix[i].length) {
				MathNumber diag = matrix[i][i];
				if (diag != null && diag.lt(MathNumber.ZERO)) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * m• ⊑DBM n ⇐⇒γ Oct(m) ⊆γ Oct(n)
	 */
	@Override
	public boolean lessOrEqualAux(
			DifferenceBoundMatrix other)
			throws SemanticException {

		if (this.isBottom()) {
			return true;
		}

		if (other.isBottom()) {
			return false;
		}

		if (this.matrix.length != other.matrix.length) {
			throw new SemanticException("Matrices have different dimensions");
		}

		// ensure both DBMs talk about the same identifier set
		for (Map.Entry<Identifier, Integer> entry : variableIndex.entrySet()) {
			if (!other.variableIndex.containsKey(entry.getKey())) {
				throw new SemanticException("Variable are not the same");
			}
		}

		DifferenceBoundMatrix first = new DifferenceBoundMatrix(copyMatrix(this.matrix),
				new java.util.HashMap<Identifier, Integer>(this.variableIndex));
		DifferenceBoundMatrix second = new DifferenceBoundMatrix(copyMatrix(other.matrix),
				new java.util.HashMap<Identifier, Integer>(other.variableIndex));
		

		/*for (int i = 0; i < first.matrix.length; i++) {
     	   for (int j = 0; j < first.matrix.length; j++) {
			if(i%2 == 0 && j%2 == 1 && j == i+1)
				{
					return true;
				}
				else if(i%2 == 1 && j%2 == 0 && i == j+1)
				{
					return true;
				}

		   }
		}*/

		
		// compute m• ⊑DBM n
		
		for (int i = 0; i < first.matrix.length; i++) {
     	   for (int j = 0; j < first.matrix.length; j++) {
            if (first.matrix[i][j].compareTo(second.matrix[i][j]) != 0) {
                return false;
            }
        }
    }
		return true;

	}

	/*
	 * (m ⊔^DBM n)i j = max(mi j, ni j)
	 */
	@Override
	public DifferenceBoundMatrix lubAux(
			DifferenceBoundMatrix other)
			throws SemanticException {
		// first check if two dbm are the same
		for (Map.Entry<Identifier, Integer> entry : variableIndex.entrySet()) {
			if (!other.variableIndex.containsKey(entry.getKey())) {
				throw new SemanticException("Variable are not the same");
			}
		}

		DifferenceBoundMatrix first = this.strongClosure();
		DifferenceBoundMatrix second = other.strongClosure();

		if (first.isBottom() && second.isBottom()) {
			return first;
		}
		if (first.isBottom()) {
			return second;
		}
		if (second.isBottom()) {
			return first;
		}

		MathNumber[][] newMatrix = new MathNumber[this.matrix.length][this.matrix.length];
		for (int i = 0; i < this.matrix.length; i++) {
			for (int j = 0; j < this.matrix.length; j++) {
				if (first.matrix[i][j].compareTo(second.matrix[i][j]) > 0) {
            		newMatrix[i][j] = first.matrix[i][j];
		        } else {
            		newMatrix[i][j] = second.matrix[i][j];
        		}
			}
		}

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(newMatrix, this.variableIndex);
		return result;
	}

	
	/*
	 * (m ⊓^DBM n)i j = min(mi j, ni j)
	 */
	@Override
	public DifferenceBoundMatrix glbAux(
			DifferenceBoundMatrix other)
			throws SemanticException {
		// first check if two dbm are the same
		for (Map.Entry<Identifier, Integer> entry : variableIndex.entrySet()) {
			if (!other.variableIndex.containsKey(entry.getKey())) {
				throw new SemanticException("Variable are not the same");
			}
		}

		MathNumber[][] newMatrix = new MathNumber[this.matrix.length][this.matrix.length];
		for (int i = 0; i < this.matrix.length; i++) {
			for (int j = 0; j < this.matrix.length; j++) {
				newMatrix[i][j] = this.matrix[i][j].min(other.matrix[i][j]);
			}
		}

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(newMatrix, this.variableIndex);
		result = result.strongClosure();
		return result;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom()) {
			return Lattice.bottomRepresentation();
		}
		if (isTop()) {
			return Lattice.topRepresentation();
		}
		String matrixStr = "\n";
		for (int i = 0; i < matrix.length; i++) {
			matrixStr += "|";
			for (int j = 0; j < matrix.length; j++) {
				matrixStr += matrix[i][j] + " ";
			}
			matrixStr += "|\n";
		}
		StringRepresentation result = new StringRepresentation(
				"DBM[" + matrix.length + "x" + matrix.length + "]\n" + matrixStr);
		return result;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		boolean result = variableIndex.containsKey(id);
		return result;
	}

	/**
	 * Performs an assignment operation in the octagon domain. This method
	 * encodes the assignment {@code id = expression} into the DBM by updating
	 * the constraint matrix. The implementation handles various assignment
	 * patterns:
	 * <ul>
	 * <li>Direct constant assignment: {@code Vi = c}</li>
	 * <li>Self-modification: {@code Vi = Vi + c}</li>
	 * <li>Variable-to-variable with offset: {@code Vi = Vj + c}</li>
	 * <li>Negation assignments: {@code Vi = -Vj + c}</li>
	 * </ul>
	 * If the identifier is not already tracked in the matrix, it is added with
	 * appropriate initialization. After encoding the assignment, strong closure
	 * is applied to maintain the canonical form.
	 * 
	 * @param id         the identifier being assigned to
	 * @param expression the expression being assigned
	 * @param pp         the program point where the assignment occurs
	 * @param oracle     the semantic oracle for resolving expression semantics
	 * 
	 * @return a new DBM reflecting the assignment
	 * 
	 * @throws SemanticException if the assignment cannot be encoded
	 */
	@Override
	public DifferenceBoundMatrix assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// Create a copy of variableIndex to work with (for immutability)
		Map<Identifier, Integer> workingVariableIndex = new java.util.HashMap<>(this.variableIndex);

		// Work on a local matrix reference so we can add a variable and still
		// continue
		MathNumber[][] curMatrix = this.matrix;

		// add a new variable to the matrix if not already present
		if (!workingVariableIndex.containsKey(id)) {
			// skip synthetic 'this' identifier used by the analysis
			String idName = id.getName();
			if (idName == null || "this".equals(idName)) {
				return this;
			}
			// new variable added
			int newIndex = workingVariableIndex.size();
			workingVariableIndex.put(id, newIndex);
			// create a new matrix with one more row and column
			int newSize = 2 * (newIndex + 1);
			MathNumber[][] newMatrix = new MathNumber[newSize][newSize];
			// create the new matrix with old values and +inf in new positions
			for (int i = 0; i < newSize; i++) {
				for (int j = 0; j < newSize; j++) {
					if (i < matrix.length && j < matrix.length) {
						newMatrix[i][j] = matrix[i][j];
					} else if (i == j) {
						newMatrix[i][j] = MathNumber.ZERO;
					} else {
						newMatrix[i][j] = MathNumber.PLUS_INFINITY;
					}
				}
			}

			curMatrix = newMatrix;
		}

		// encode the assignment expression into the matrix
		// Vi = c
		// encoded as :
		// Vi <= c : V^'_(2i-1) - V^'_(2i) <= 2c
		// Vi >= c : V^'_(2i) - V^'_(2i-1) <= -2c
		int newVariableIndex = workingVariableIndex.size();
		if (hasVariable(expression).equals("")) {
			firstCaseAssignement(curMatrix, newVariableIndex, expression, id, workingVariableIndex);
		} else if (expression instanceof BinaryExpression && hasVariable(expression).equals(id.getName())) {
			// Case 2: Vi0 = Vi0 + constant
			secondCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, 0, false);

		} else if (!hasVariable(expression).equals(id.getName())
				&& hasVariable(expression).charAt(0) != '-') {
			// Case 3: Vi0 = Vj0 + constant with i0 != j0

			Double c = 0.0;
			if (expression instanceof BinaryExpression) {
				try {
					c = resolveCostantExpression(((ValueExpression) ((BinaryExpression) expression).getRight()));

					if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
						c = -c;
					}

					thirdCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, c,
							false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if (!workingVariableIndex.containsKey(id)) {
					thirdCaseAssignement(curMatrix, newVariableIndex, expression, id, c, true);
				} else {
					thirdCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, c, true);
				}

			}
		} else if (hasVariable(expression).equals("- " + id.getName())) {
			fourthCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id);
		} else {

			if (!hasVariable(expression).contains("- " + id.getName())
					&& (expression.toString().split("- ").length == 2
							&& expression.toString().split(Pattern.quote("+")).length == 1)) {
				thirdCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, 0, true);

				fourthCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id);

			} else if (hasVariable(expression).contains("- " + id.getName())
					&& expression.toString().split("- " + id.getName()).length > 1) {
				double offset = 0;

				if (expression.toString().split("- ").length > 2) {
					offset = Double.parseDouble(expression.toString().split("- ")[2]);
				} else {
					offset = -Double.parseDouble(expression.toString().split("- ")[1].split("\\+")[1]);
				}

				secondCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, offset,
						true);
				fourthCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id);

			} else {
				double offset = 0;

				if (expression.toString().split("- ").length > 2) {
					offset = Double.parseDouble(expression.toString().split("- ")[2]);
				} else {
					offset = -Double.parseDouble(expression.toString().split("- ")[1].split("\\+")[1]);
				}

				secondCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, offset,
						true);
				fourthCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id);
				thirdCaseAssignement(curMatrix, idToPos(id, workingVariableIndex) / 2 + 1, expression, id, 0, true);

			}
		}

		// convert zeros to MathNumber.ZERO
		for (int i = 0; i < curMatrix.length; i++) {
			for (int j = 0; j < curMatrix.length; j++) {
				if (curMatrix[i][j] != null && curMatrix[i][j].isZero()) {
					curMatrix[i][j] = MathNumber.ZERO;
				}
			}
		}

		Floyd.strongClosureFloyd(curMatrix);
		DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix, workingVariableIndex);
		// Floyd.printMatrix(result.matrix);
	
		//System.out.println(result.representation());
		return result;

	}

	@Override
	public DifferenceBoundMatrix smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	/**
	 * Refines this DBM by assuming a boolean expression holds. This method
	 * encodes conditional constraints into the matrix, narrowing the possible
	 * values of variables based on the assumed condition. The implementation
	 * normalizes the expression and handles various constraint forms:
	 * <ul>
	 * <li>Logical conjunctions (AND): applies both constraints and takes their
	 * glb</li>
	 * <li>Logical disjunctions (OR): applies both constraints and takes their
	 * lub</li>
	 * <li>Difference constraints: {@code b - a ≤ c}</li>
	 * <li>Variable bounds: {@code x ≤ c} or {@code x ≥ c}</li>
	 * </ul>
	 * This operation is crucial for handling conditional branches and loop
	 * conditions in program analysis.
	 * 
	 * @param expression the boolean expression to assume
	 * @param src        the source program point
	 * @param dest       the destination program point
	 * @param oracle     the semantic oracle for resolving expression semantics
	 * 
	 * @return a new DBM refined by the assumed constraint
	 * 
	 * @throws SemanticException if the constraint cannot be encoded
	 */
	@Override
	public DifferenceBoundMatrix assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {

		SymbolicExpression normalized = BooleanExpressionNormalizer.normalize(expression, src.getLocation(),
				src, oracle);
		if (!(normalized instanceof BinaryExpression)) {
			return this;
		}

		BinaryExpression be = (BinaryExpression) normalized;

		if (be.getOperator().equals(LogicalAnd.INSTANCE)) {
			// apply assume for both sides and then merge the results with glb
			DifferenceBoundMatrix leftResult = this.assume((ValueExpression) be.getLeft(), src, dest, oracle);
			DifferenceBoundMatrix rightResult = this.assume((ValueExpression) be.getRight(), src, dest, oracle);

			return leftResult.glbAux(rightResult);
		}

		if (be.getOperator().equals(LogicalOr.INSTANCE)) {
			// apply assume for both sides and then merge the results with lub
			DifferenceBoundMatrix leftResult = this.assume((ValueExpression) be.getLeft(), src, dest, oracle);
			DifferenceBoundMatrix rightResult = this.assume((ValueExpression) be.getRight(), src, dest, oracle);
			return leftResult.lubAux(rightResult);
		}

		if (!be.getOperator().equals(ComparisonLe.INSTANCE)) {

			return this
					.addConstraint((Identifier) (((BinaryExpression) be.getLeft()).getRight()),
							new MathNumber(Double.parseDouble(((BinaryExpression) be.getLeft()).getLeft().toString())),
							false);
		}

		SymbolicExpression left = be.getLeft();
		SymbolicExpression right = be.getRight();

		// Handle (something) <= constant
		if (!(right instanceof Constant) || !(left instanceof BinaryExpression)) {
			return this;
		}

		BinaryExpression sub = (BinaryExpression) left;
		// Case b - a <= c
		if (sub.getOperator().equals(NumericNonOverflowingSub.INSTANCE)) {

			SymbolicExpression b = sub.getLeft();
			SymbolicExpression a = sub.getRight();
			Constant c = (Constant) right;

			MathNumber cVal = toMathNumber(c.getValue());
			if (cVal == null)
				return this;

			if (a instanceof Identifier && b instanceof UnaryExpression) {
				// -b - a <= c
				UnaryExpression ub = (UnaryExpression) b;
				if (ub.getOperator().equals(NumericNegation.INSTANCE)
						&& ub.getExpression() instanceof Identifier) {
					return this.addConstraint((Identifier) a, (Identifier) ub.getExpression(), cVal, true, true);
				}
			}

			if (a instanceof Identifier && b instanceof Identifier) {
				// b - a <= c
				return this.addConstraint((Identifier) a, (Identifier) b, cVal, false, true);
			}

			if (a instanceof Constant && b instanceof Identifier) {
				// b - const <= c
				MathNumber k = toMathNumber(((Constant) a).getValue());
				if (k == null)
					return this;
				MathNumber ub = cVal.add(k);

				return this.addConstraint((Identifier) b, ub.multiply(new MathNumber(-1)), false);
			}

			if (a instanceof Identifier && b instanceof Constant) {
				// const - a <= c
				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (k == null)
					return this;
				MathNumber val = cVal.subtract(k);
				return this.addConstraint((Identifier) a, val.multiply(new MathNumber(-1)), true);
			}

			// handle the case x - y - c1 <= c2
			// Objective: call addConstraint(Identifier a, Identifier b,
			// MathNumber c)
			if (a instanceof Constant && b instanceof SymbolicExpression) {
				MathNumber c1 = toMathNumber(((Constant) a).getValue());
				if (c1 == null)
					return this;

				MathNumber adjusted = cVal.add(c1);

				if (b instanceof UnaryExpression) {
					UnaryExpression ub = (UnaryExpression) b;
					if (ub.getOperator().equals(NumericNegation.INSTANCE)
							&& ub.getExpression() instanceof Identifier) {
						return this.addConstraint((Identifier) ub.getExpression(), adjusted, true);
					}
				}

				if (!(b instanceof BinaryExpression)) {
					return this;
				}

				BinaryExpression bExpr = (BinaryExpression) b;
				Identifier b1 = bExpr.getLeft() instanceof Identifier ? (Identifier) bExpr.getLeft() : null;
				Identifier b2 = bExpr.getRight() instanceof Identifier ? (Identifier) bExpr.getRight() : null;
				if (b1 == null || b2 == null)
					return this;

				if (bExpr.getOperator().equals(NumericNonOverflowingSub.INSTANCE)) {
					// b1 - b2 - const <= 0
					// b1 - b2 <= const
					DifferenceBoundMatrix r = this.addConstraint(b1, b2, adjusted, false, true);
					r.representation().toString();
					return r;

				} else if (bExpr.getOperator().equals(NumericNonOverflowingAdd.INSTANCE)) {
					// -b1 + b2 - const <= 0
					// -b1 + b2 <= const
					// b2 - b1 <= const
					DifferenceBoundMatrix r = this.addConstraint(b1, b2, adjusted, true, false);
					r.representation().toString();
					return r;
				}

			}
		}

		// Case -b + a <= c
		if (sub.getOperator().equals(NumericNonOverflowingAdd.INSTANCE)) {
			SymbolicExpression a = sub.getLeft();
			SymbolicExpression b = sub.getRight();
			Constant c = (Constant) right;

			MathNumber cVal = toMathNumber(c.getValue());
			if (cVal == null)
				return this;

			if (a instanceof BinaryExpression && ((BinaryExpression) a).getOperator()
					.equals(NumericNonOverflowingSub.INSTANCE) && b instanceof Constant) {
				BinaryExpression diff = (BinaryExpression) a;
				Identifier leftId = diff.getLeft() instanceof Identifier ? (Identifier) diff.getLeft() : null;
				Identifier rightId = diff.getRight() instanceof Identifier ? (Identifier) diff.getRight() : null;
				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (leftId != null && rightId != null && k != null) {
					MathNumber adjusted = cVal.subtract(k);
					return this.addConstraint(rightId, leftId, adjusted, false, true);
				}
			}

			if (b instanceof BinaryExpression && ((BinaryExpression) b).getOperator()
					.equals(NumericNonOverflowingSub.INSTANCE) && a instanceof Constant) {
				BinaryExpression diff = (BinaryExpression) b;
				Identifier leftId = diff.getLeft() instanceof Identifier ? (Identifier) diff.getLeft() : null;
				Identifier rightId = diff.getRight() instanceof Identifier ? (Identifier) diff.getRight() : null;
				MathNumber k = toMathNumber(((Constant) a).getValue());
				if (leftId != null && rightId != null && k != null) {
					MathNumber adjusted = cVal.subtract(k);
					return this.addConstraint(rightId, leftId, adjusted, false, true);
				}
			}

			// -x + const <= c
			if (a instanceof UnaryExpression && b instanceof Constant) {
				UnaryExpression ua = (UnaryExpression) a;
				if (!ua.getOperator().equals(NumericNegation.INSTANCE) || !(ua.getExpression() instanceof Identifier)) {
					return this;
				}

				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (k == null)
					return this;

				MathNumber val = cVal.subtract(k);
				return this.addConstraint((Identifier) ua.getExpression(), val.multiply(new MathNumber(-1)), true);
			}

			// const + (-x) <= c
			if (a instanceof Constant && b instanceof UnaryExpression) {
				UnaryExpression ub = (UnaryExpression) b;
				if (!ub.getOperator().equals(NumericNegation.INSTANCE) || !(ub.getExpression() instanceof Identifier)) {
					return this;
				}

				MathNumber k = toMathNumber(((Constant) a).getValue());
				if (k == null)
					return this;

				MathNumber val = cVal.subtract(k);
				return this.addConstraint((Identifier) ub.getExpression(), val.multiply(new MathNumber(-1)), true);
			}

			if (a instanceof Identifier && b instanceof Constant) {
				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (k == null)
					return this;

				MathNumber ub = cVal.subtract(k);
				return this.addConstraint((Identifier) a, ub.multiply(new MathNumber(-1)), false);
			}

			if (a instanceof Constant && b instanceof Identifier) {
				MathNumber k = toMathNumber(((Constant) a).getValue());
				if (k == null)
					return this;

				MathNumber ub = cVal.subtract(k);
				return this.addConstraint((Identifier) b, ub.multiply(new MathNumber(-1)), false);
			}

			if (a instanceof Identifier && b instanceof Identifier) {
				return this.addConstraint((Identifier) a, (Identifier) b, cVal, false, false);
			}

			// -x + y <= c <=> y - x <= c
			if (a instanceof UnaryExpression && b instanceof Identifier) {
				UnaryExpression ua = (UnaryExpression) a;
				if (!ua.getOperator().equals(NumericNegation.INSTANCE) || !(ua.getExpression() instanceof Identifier)) {
					return this;
				}
				return this.addConstraint((Identifier) ua.getExpression(), (Identifier) b, cVal, false, true);
			}

			// x + (-y) <= c <=> x - y <= c
			if (a instanceof Identifier && b instanceof UnaryExpression) {
				UnaryExpression ub = (UnaryExpression) b;
				if (!ub.getOperator().equals(NumericNegation.INSTANCE) || !(ub.getExpression() instanceof Identifier)) {
					return this;
				}
				return this.addConstraint((Identifier) ub.getExpression(), (Identifier) a, cVal, false, true);
			}
		}

		return this;
	}

	/**
	 * Forgets (removes constraints on) a specific identifier from the DBM. This
	 * operation sets all constraints involving the identifier to their most
	 * permissive values (infinity), effectively removing any information about
	 * the variable while maintaining consistency for other variables. This is
	 * typically used when a variable goes out of scope or when its value
	 * becomes unknown.
	 * 
	 * @param id the identifier to forget
	 * 
	 * @return this DBM after forgetting the identifier (modified in place)
	 * 
	 * @throws SemanticException if the forget operation fails
	 */
	@Override
	public DifferenceBoundMatrix forgetIdentifier(
			Identifier id)
			throws SemanticException {
		if (!variableIndex.containsKey(id)) {
			return this;
		}
		int pos = idToPos(id, this.variableIndex) + 1;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				if (i != 2 * pos - 2 && i != 2 * pos - 1 && j != 2 * pos - 2 && j != 2 * pos - 1) {
					matrix[i][j] = matrix[i][j];
				} else if ((i == j && i == 2 * pos - 2) || (i == j && i == 2 * pos - 1)) {
					matrix[i][j] = MathNumber.ZERO;
				} else {
					matrix[i][j] = MathNumber.PLUS_INFINITY;
				}
			}
		}

		return this;
	}

	@Override
	public DifferenceBoundMatrix forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		// For now, return this as a placeholder
		return this;
	}

	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// For now, return unknown as a safe approximation
		return Satisfiability.UNKNOWN;
	}

	@Override
	public DifferenceBoundMatrix pushScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public DifferenceBoundMatrix popScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	/**
	 * Computes the normal closure of this DBM using the standard Floyd-Warshall
	 * algorithm. The closure ensures all transitive constraints are made
	 * explicit in the matrix. Unlike {@link #strongClosure()}, this does not
	 * enforce octagon-specific coherence properties.
	 * 
	 * @return this DBM after applying closure (modified in place)
	 * 
	 * @throws SemanticException if the closure computation fails
	 */
	public DifferenceBoundMatrix closure() throws SemanticException {
		Floyd.Floyd(this.matrix, new MathNumber[matrix.length][matrix.length]);
		return this;
	}

	/**
	 * Computes the strong closure of this difference-bound matrix. The strong
	 * closure ensures that the matrix represents the tightest possible set of
	 * constraints that are implied by the current constraints. This operation
	 * uses a specialized Floyd-Warshall algorithm optimized for octagon
	 * constraints.
	 * <p>
	 * The strong closure must be applied when:
	 * <ul>
	 * <li>Testing inclusion (lessOrEqual) or equality (equals)</li>
	 * <li>Computing abstract union (lub)</li>
	 * <li>Converting to interval domain (toInterval)</li>
	 * <li>Performing exact forget operations (forgetIdentifier)</li>
	 * <li>Maximizing accuracy in potentially inaccurate operations</li>
	 * </ul>
	 * </p>
	 * 
	 * @return this DBM after applying strong closure (modified in place)
	 * 
	 * @throws SemanticException if the closure computation fails
	 */
	public DifferenceBoundMatrix strongClosure() throws SemanticException {
		Floyd.strongClosureFloyd(this.matrix);
		return this;
	}

	/**
	 * Converts this difference-bound matrix to an interval domain environment.
	 * This projection extracts the bounds for each variable by reading the
	 * appropriate entries from the closed DBM. The formal definition is: πi(m)
	 * = ∅ if m• = ⊥DBM (empty octagon), or [−m•(2i−1)(2i)/2, m•(2i)(2i−1)/2] if
	 * m• ≠ ⊥DBM.
	 * 
	 * @return the interval environment with bounds for each variable
	 * 
	 * @throws SemanticException if the conversion fails
	 */
	public ValueEnvironment<Interval> toInterval() throws SemanticException {
		ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());
		DifferenceBoundMatrix dbm = this.closure(); // apply the closure to
													// ensure the matrix is in
													// normal form
		if (dbm.isBottom()) {
			return env;
		}
		for (Identifier id : variableIndex.keySet()) {
			int pos = idToPos(id, variableIndex);
			int neg = idToNeg(id, variableIndex);
			MathNumber upperBound = dbm.matrix[neg][pos]; // m•(2i)(2i−1)
			MathNumber lowerBound = dbm.matrix[pos][neg]; // m•(2i−1)(2i)
			Interval interval;
			if (upperBound == null || lowerBound == null) {
				interval = new Interval().top();
			} else {
				interval = new Interval(lowerBound.divide(new MathNumber(-2)), upperBound.divide(new MathNumber(2)));
			}
			env = env.putState(id, interval);
		}
		return env;
	}

	/**
	 * Converts an interval domain environment to a difference-bound matrix.
	 * This lifting operation creates octagon constraints from non-relational
	 * interval bounds. The formal definition is: (Oct(X))_ij = 2 × snd(X(Vk))
	 * if i = 2k, j = 2k − 1; −2 × fst(X(Vk)) if j = 2k, i = 2k − 1; +∞
	 * otherwise.
	 * 
	 * @param env the interval environment to convert
	 * 
	 * @return the DBM representation of the interval constraints
	 * 
	 * @throws SemanticException if the conversion fails
	 */
	public static DifferenceBoundMatrix fromIntervalDomain(
			ValueEnvironment<Interval> env)
			throws SemanticException {
		Map<Identifier, Integer> variableIndex = new java.util.HashMap<Identifier, Integer>();
		int n = env.getKeys().size();
		MathNumber[][] matrix = new MathNumber[2 * n][2 * n];
		// initialize the matrix with +inf
		for (int i = 0; i < 2 * n; i++) {
			for (int j = 0; j < 2 * n; j++) {
				if (i == j) {
					matrix[i][j] = MathNumber.ZERO; // distance to self is 0
				} else {
					matrix[i][j] = MathNumber.PLUS_INFINITY; // unknown
																// distances are
																// +inf
				}
			}
		}
		int index = 0;
		for (Identifier id : env.getKeys()) {
			variableIndex.put(id, index);
			Interval interval = env.getState(id);
			if (interval != null) {
				MathNumber lower = interval.interval.getLow();
				MathNumber upper = interval.interval.getHigh();
				int pos = 2 * index; // V'_(2i - 1)
				int neg = 2 * index + 1; // V'_(2i)
				if (upper != null && !upper.isPlusInfinity()) {
					matrix[neg][pos] = upper.multiply(new MathNumber(2)); // 2 *
																			// snd(X(Vk))
				}
				if (lower != null && !lower.isMinusInfinity()) {
					matrix[pos][neg] = lower.multiply(new MathNumber(-2)); // -2
																			// *
																			// fst(X(Vk))
				}
			}
			index++;
		}
		DifferenceBoundMatrix result = new DifferenceBoundMatrix(matrix, variableIndex);
		return result;
	}

	// Convert a generic object (coming from Constant.getValue()) into
	// MathNumber,
	// if possible
	private MathNumber toMathNumber(
			Object v) {
		if (v == null)
			return null;
		if (v instanceof MathNumber)
			return (MathNumber) v;
		if (v instanceof Integer)
			return new MathNumber((Integer) v);
		if (v instanceof Long)
			return new MathNumber(((Long) v).doubleValue());
		if (v instanceof Double)
			return new MathNumber((Double) v);
		if (v instanceof Float)
			return new MathNumber(((Float) v).doubleValue());
		try {
			// last resort: parse from string
			return new MathNumber(Double.parseDouble(v.toString()));
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Maps a program variable identifier to its positive index in the matrix.
	 * For a variable Vi with index i in the variable map, this returns the
	 * index V'_(2i-1) = 2i in the DBM matrix.
	 * 
	 * @param id       the identifier to map
	 * @param varIndex the variable index map
	 * 
	 * @return the positive matrix index for the variable
	 * 
	 * @throws IllegalArgumentException if the identifier is not in the variable
	 *                                      index
	 */
	public int idToPos(
			Identifier id,
			Map<Identifier, Integer> varIndex) {
		if (!varIndex.containsKey(id))
			throw new IllegalArgumentException("Identifier " + id + " not found in variable index.");
		int i = varIndex.get(id);
		int result = (i == 0) ? 0 : 2 * i;
		return result;
	}

	/**
	 * Maps a program variable identifier to its negative index in the matrix.
	 * For a variable Vi with index i in the variable map, this returns the
	 * index V'_(2i) = 2i+1 in the DBM matrix, representing -Vi.
	 * 
	 * @param id       the identifier to map
	 * @param varIndex the variable index map
	 * 
	 * @return the negative matrix index for the variable
	 * 
	 * @throws IllegalArgumentException if the identifier is not in the variable
	 *                                      index
	 */
	public int idToNeg(
			Identifier id,
			Map<Identifier, Integer> varIndex) {
		if (!varIndex.containsKey(id))
			throw new IllegalArgumentException("Identifier " + id + " not found in variable index.");
		int i = varIndex.get(id);
		int result = 2 * i + 1;
		return result;
	}

	// Helper function to add a constraint of the type x - c <= 0
	// m(2k)(2k-1) = min(m(2k)(2k-1), 2*c)
	// In negated case we have a constraint of the type -x - c <= 0
	// m(2k-1)(2k) = min(m(2k-1)(2k), 2*c)
	private DifferenceBoundMatrix addConstraint(
			Identifier a,
			MathNumber c,
			boolean isNegated)
			throws SemanticException {

		// Work on a copy of the matrix
		MathNumber[][] curMatrix = copyMatrix(this.matrix);

		int i = 0;
		int j = 0;

		if (isNegated) {
			i = idToPos(a, variableIndex);
			j = idToNeg(a, variableIndex);
		} else {
			i = idToNeg(a, variableIndex);
			j = idToPos(a, variableIndex);
		}

		curMatrix[i][j] = c.multiply(new MathNumber(-2));
		DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix, this.variableIndex);
		return result;

	}

	/**
	 * Adds a relational constraint of the form {@code ±b ± a ≤ c} to the DBM.
	 * This helper method encodes difference constraints between two variables
	 * into the matrix. The negation flags determine which form of the
	 * constraint is added:
	 * <ul>
	 * <li>{@code b - a ≤ c} (both false)</li>
	 * <li>{@code -b - a ≤ c} (firstNegated=true, secondNegated=false)</li>
	 * <li>{@code b + a ≤ c} (firstNegated=false, secondNegated=true)</li>
	 * <li>{@code -b + a ≤ c} (both true)</li>
	 * </ul>
	 * The constraint is added by updating the appropriate matrix entries to
	 * maintain the tightest bounds.
	 * 
	 * @param a             the first variable
	 * @param b             the second variable
	 * @param c             the constant bound
	 * @param firstNegated  whether the first variable is negated
	 * @param secondNegated whether the second variable is negated
	 * 
	 * @return a new DBM with the constraint added
	 * 
	 * @throws SemanticException if the constraint cannot be added
	 */
	private DifferenceBoundMatrix addConstraint(
			Identifier a,
			Identifier b,
			MathNumber c,
			boolean firstNegated,
			boolean secondNegated)
			throws SemanticException {

		// Work on a copy of the matrix
		MathNumber[][] curMatrix = copyMatrix(this.matrix);

		// Add the constraint b - a <= c
		/*
		 * V_j0 - V_i0 <= c = m[2i-1][2j-1] = min(m[2i-1][2j-1], 2*c) (1)
		 * m[2j][2i] = min(m[2j][2i], 2*c) (2) Vj0 + Vi0 <= c = m[2i][2j-1] =
		 * min(m[2i][2j-1], 2*c) (3) m[2j][2i] = min(m[2j][2i], 2*c) (4) -Vj0 -
		 * Vi0 <= c = m[2i-1][2j] = min(m[2i-1][2j], 2*c) (5) m[2j-1][2i] =
		 * min(m[2j-1][2i], 2*c) (6)
		 */
		int posA = firstNegated ? idToNeg(a, variableIndex) : idToPos(a, variableIndex);
		int negA = firstNegated ? idToPos(a, variableIndex) : idToNeg(a, variableIndex);
		int posB = secondNegated ? idToNeg(b, variableIndex) : idToPos(b, variableIndex);
		int negB = secondNegated ? idToPos(b, variableIndex) : idToNeg(b, variableIndex);

		curMatrix[negA][posB] = curMatrix[negA][posB].min(c);

		curMatrix[negB][posA] = curMatrix[negB][posA].min(c);

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix, this.variableIndex);
		return result;
	}

	/**
	 * Computes the widening of this DBM with another DBM. Widening is a
	 * critical operation for ensuring termination of fixpoint computations in
	 * program analysis. The implementation compares corresponding matrix
	 * entries:
	 * <ul>
	 * <li>If {@code this[i][j] < other[i][j]}, the constraint is weakening, so
	 * set to +∞ (no constraint)</li>
	 * <li>If {@code this[i][j] > other[i][j]}, the constraint is strengthening,
	 * so set to -∞ (inconsistent)</li>
	 * <li>Otherwise, keep the current value</li>
	 * </ul>
	 * This ensures that the widening eventually stabilizes by forcing
	 * constraints to their limit values when they keep changing.
	 * 
	 * @param other the DBM to widen with
	 * 
	 * @return a new DBM representing the widening of this and other
	 * 
	 * @throws SemanticException if the widening operation fails
	 */
	@Override
	public DifferenceBoundMatrix wideningAux(
			DifferenceBoundMatrix other)
			throws SemanticException {
		if (other == null) {
			return this;
		}

		int size = this.matrix.length;
		MathNumber[][] resultMatrix = new MathNumber[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// Widening: if this[i][j] < other[i][j], the constraint is
				// getting weaker, so
				// set to +∞
				
				if(i != j)
				{
				
					if(i%2 == 0 && j%2 == 1 && j == i+1 && this.matrix[i][j].compareTo(this.matrix[j][i].multiply(new MathNumber(-1))) != 0)
					{
						if(this.matrix[i][j].isPositive() && this.matrix[i][j].compareTo(this.matrix[j][i]) > 0)
						{
							resultMatrix[i][j] =  this.matrix[i][j].min(this.matrix[j][i]);
							resultMatrix[i][j] = resultMatrix[i][j].multiply(new MathNumber(-1));
							resultMatrix[j][i] = resultMatrix[i][j].multiply(new MathNumber(-1));
												
						}
						else
						{
							resultMatrix[j][i] =  this.matrix[j][i].max(this.matrix[i][j]);
							resultMatrix[i][j] = resultMatrix[j][i].multiply(new MathNumber(-1));
							resultMatrix[j][i] = resultMatrix[j][i].multiply(new MathNumber(-1));
							resultMatrix[i][j] = resultMatrix[j][i].multiply(new MathNumber(-1));
						}
					}
					else if(i%2 == 1 && j%2 == 0 && i == j+1)
					{
						resultMatrix[i][j] = this.matrix[j][i];
						resultMatrix[j][i] = this.matrix[i][j];
					}
					else
					{
						resultMatrix[i][j] = MathNumber.PLUS_INFINITY;
					}
				}
				else
				{
					resultMatrix[i][j] = MathNumber.ZERO;
				}
			}
		}

		
		
		Floyd.strongClosureFloyd(resultMatrix);
		final DifferenceBoundMatrix result = new DifferenceBoundMatrix(resultMatrix, this.variableIndex);
		return result;

	}


	private double resolveVariableExpression(
			ValueExpression exp)
			throws MathNumberConversionException {
		for (Identifier key : this.variableIndex.keySet()) {
			if (key.toString().equals(exp.toString())) {
				return this.matrix[idToPos(key, this.variableIndex)][idToNeg(key, this.variableIndex)].toDouble();
			}
		}

		return Double.NaN;
	}

	// It is assumed that the expression can have at most one variable
	private String hasVariable(
			ValueExpression exp) {
		if (isDouble(exp.toString())) {
			return "";
		} else if (exp instanceof BinaryExpression) {
			BinaryExpression bExp = ((BinaryExpression) exp);

			return hasVariable((ValueExpression) bExp.getLeft()) + hasVariable((ValueExpression) bExp.getRight());
		}

		return exp.toString();
	}

	/**
	 * Recursively evaluates a constant expression to compute its numeric value.
	 * This method handles arithmetic operations (+, -, *, /) on numeric
	 * constants, performing recursive evaluation for nested binary expressions.
	 * 
	 * @param exp the constant expression to evaluate
	 * 
	 * @return the numeric value of the expression, or 0 if evaluation fails
	 * 
	 * @throws MathNumberConversionException if number conversion fails during
	 *                                           evaluation
	 */
	private double resolveCostantExpression(
			ValueExpression exp)
			throws MathNumberConversionException {
		// Base cases
		if (isDouble(exp.toString())) {
			return Double.parseDouble(exp.toString());
		} else if (exp instanceof BinaryExpression) {
			BinaryExpression bExp = (BinaryExpression) exp;

			if (bExp.getOperator().toString().equals("+")) {
				if (isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							+ Double.parseDouble(bExp.getRight().toString());
				} else if (isDouble(bExp.getLeft().toString()) && !isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							+ resolveCostantExpression((ValueExpression) bExp.getRight());
				} else if (!isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							+ Double.parseDouble(bExp.getRight().toString());
				} else {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							+ resolveCostantExpression((ValueExpression) bExp.getRight());
				}
			} else if (bExp.getOperator().toString().equals("-")) {
				if (isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							- Double.parseDouble(bExp.getRight().toString());
				} else if (isDouble(bExp.getLeft().toString()) && !isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							- resolveCostantExpression((ValueExpression) bExp.getRight());
				} else if (!isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							- Double.parseDouble(bExp.getRight().toString());
				} else {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							- resolveCostantExpression((ValueExpression) bExp.getRight());
				}
			} else if (bExp.getOperator().toString().equals("*")) {
				if (isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							* Double.parseDouble(bExp.getRight().toString());
				} else if (isDouble(bExp.getLeft().toString()) && !isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							* resolveCostantExpression((ValueExpression) bExp.getRight());
				} else if (!isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							* Double.parseDouble(bExp.getRight().toString());
				} else {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							* resolveCostantExpression((ValueExpression) bExp.getRight());
				}
			} else if (bExp.getOperator().toString().equals("/")) {
				if (isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							/ Double.parseDouble(bExp.getRight().toString());
				} else if (isDouble(bExp.getLeft().toString()) && !isDouble(bExp.getRight().toString())) {
					return Double.parseDouble(bExp.getLeft().toString())
							/ resolveCostantExpression((ValueExpression) bExp.getRight());
				} else if (!isDouble(bExp.getLeft().toString()) && isDouble(bExp.getRight().toString())) {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							/ Double.parseDouble(bExp.getRight().toString());
				} else {
					return resolveCostantExpression((ValueExpression) bExp.getLeft())
							/ resolveCostantExpression((ValueExpression) bExp.getRight());
				}
			}
		}

		return 0;
	}

	public static boolean isDouble(
			String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Handles the fourth case of assignment: negation assignments like
	 * {@code Vi = -Vj} or {@code Vi = -Vi}. This operation swaps the roles of
	 * the positive and negative indices for the variable in the matrix,
	 * effectively implementing the negation constraint.
	 * 
	 * @param curMatrix        the current matrix being modified
	 * @param newVariableIndex the index of the variable being assigned
	 * @param expression       the assignment expression
	 * @param id               the identifier being assigned to
	 */
	public void fourthCaseAssignement(
			MathNumber[][] curMatrix,
			int newVariableIndex,
			ValueExpression expression,
			Identifier id) {

		MathNumber[][] matrixTmp = new MathNumber[curMatrix.length][curMatrix.length];

		Floyd.copyArray(matrixTmp, curMatrix);

		for (int i = 0; i < curMatrix.length; i++) {
			for (int j = 0; j < curMatrix.length; j++) {
				if ((i == 2 * newVariableIndex - 2 || i == 2 * newVariableIndex - 1)
						&& (j != 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 1)) {
					if (i % 2 == 0) {
						curMatrix[i][j] = matrixTmp[i + 1][j];
					} else if (i % 2 == 1) {
						curMatrix[i][j] = matrixTmp[i - 1][j];
					}
				} else if ((i != 2 * newVariableIndex - 2 && i != 2 * newVariableIndex - 1)
						&& (j == 2 * newVariableIndex - 2 || j == 2 * newVariableIndex - 1)) {
					if (j % 2 == 0) {
						curMatrix[i][j] = matrixTmp[i][j + 1];
					} else if (j % 2 == 1) {
						curMatrix[i][j] = matrixTmp[i][j - 1];
					}
				} else if ((i == 2 * newVariableIndex - 2 || i == 2 * newVariableIndex - 1)
						&& (j == 2 * newVariableIndex - 2 || j == 2 * newVariableIndex - 1)) {

					if (i % 2 == 0 && j % 2 == 0) {
						curMatrix[i][j] = matrixTmp[i + 1][j + 1];
					}
					if (i % 2 == 1 && j % 2 == 0) {
						curMatrix[i][j] = matrixTmp[i - 1][j + 1];
					}
					if (i % 2 == 0 && j % 2 == 1) {
						curMatrix[i][j] = matrixTmp[i + 1][j - 1];
					}
					if (i % 2 == 1 && j % 2 == 1) {
						curMatrix[i][j] = matrixTmp[i - 1][j - 1];
					}
				}
			}
		}

	}

	/**
	 * Handles the third case of assignment: variable-to-variable with offset
	 * like {@code Vi = Vj + c} where {@code i ≠ j}. This method updates the
	 * matrix by transferring constraints from the source variable to the target
	 * variable, adjusted by the constant offset. It performs strong closure on
	 * a copy of the matrix to ensure all transitive constraints are considered.
	 * 
	 * @param curMatrix        the current matrix being modified
	 * @param newVariableIndex the index of the variable being assigned
	 * @param expression       the assignment expression
	 * @param id               the identifier being assigned to
	 * @param offset           the constant offset value
	 * @param isOffset         whether an explicit offset is provided
	 */
	public void thirdCaseAssignement(
			MathNumber[][] curMatrix,
			int newVariableIndex,
			ValueExpression expression,
			Identifier id,
			double offset,
			boolean isOffset) {

		MathNumber[][] copyMatrix = new MathNumber[curMatrix.length][curMatrix.length];

		Floyd.copyArray(copyMatrix, curMatrix);
		Floyd.strongClosureFloyd(copyMatrix);

		MathNumber value = new MathNumber(0);
		String otherVariabileName;

		if (!isOffset) {
			otherVariabileName = hasVariable(expression);
		} else {
			if (expression.toString().split(" ").length > 1) {
				otherVariabileName = expression.toString().split(" ")[1];
			} else {
				otherVariabileName = expression.toString().split(" ")[0];
			}
		}

		int indexOtherVariable = 0;

		for (Identifier i : this.variableIndex.keySet()) {
			if (i.getName().equals(otherVariabileName)) {
				indexOtherVariable = (idToPos(i, this.variableIndex)) / 2 + 1;
				break;
			}
		}

		try {
			if (!isOffset && expression instanceof Variable) {
				value = new MathNumber(resolveVariableExpression((ValueExpression) expression));

			} else if (!isOffset && expression instanceof BinaryExpression
					&& !hasVariable((ValueExpression) ((BinaryExpression) expression).getLeft()).equals("")) {
				value = new MathNumber((Double) resolveCostantExpression(
						(ValueExpression) ((BinaryExpression) expression).getRight()));

				if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
					value = new MathNumber(-value.toDouble());
				}
			} else if (!isOffset && expression instanceof BinaryExpression) {
				value = new MathNumber(
						(Double) resolveCostantExpression((ValueExpression) ((BinaryExpression) expression).getLeft()));

				if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
					value = new MathNumber(-value.toDouble());
				}

			} else {
				value = new MathNumber(offset);
			}

		} catch (MathNumberConversionException ex) {
		}

		for (int i = 0; i < curMatrix.length; i++) {
			for (int j = 0; j < curMatrix.length; j++) {
				if ((i == 2 * newVariableIndex - 2 && j == 2 * indexOtherVariable - 2) ||
						(i == 2 * indexOtherVariable - 1 && j == 2 * newVariableIndex - 1)) {
					curMatrix[i][j] = value.subtract(value.multiply(new MathNumber(2)));
				} else if ((i == 2 * indexOtherVariable - 2 && j == 2 * newVariableIndex - 2) ||
						(i == 2 * newVariableIndex - 1 && j == 2 * indexOtherVariable - 1)) {
					curMatrix[i][j] = value;
				} else {
					try {
						curMatrix[i][j] = (new DifferenceBoundMatrix(copyMatrix, this.variableIndex))
								.forgetIdentifier(id).matrix[i][j];
					} catch (SemanticException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Handles the second case of assignment: self-modification with a constant
	 * like {@code Vi = Vi + c} or {@code Vi = Vi - c}. This method adjusts all
	 * constraints involving the variable by adding or subtracting the constant
	 * offset from the appropriate matrix entries. The offset effectively shifts
	 * the bounds of the variable.
	 * 
	 * @param curMatrix        the current matrix being modified
	 * @param newVariableIndex the index of the variable being assigned
	 * @param expression       the assignment expression
	 * @param id               the identifier being assigned to
	 * @param offset           the constant offset value
	 * @param isOffset         whether an explicit offset is provided
	 */
	public void secondCaseAssignement(
			MathNumber[][] curMatrix,
			int newVariableIndex,
			ValueExpression expression,
			Identifier id,
			double offset,
			boolean isOffset) {
		MathNumber value = new MathNumber(0);

		try {
			if (!isOffset && !hasVariable((ValueExpression) ((BinaryExpression) expression).getLeft()).equals("")) {
				value = new MathNumber((Double) resolveCostantExpression(
						(ValueExpression) ((BinaryExpression) expression).getRight()));

				if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
					value = value.subtract(value.multiply(new MathNumber(2)));
				}

			} else if (!isOffset) {
				value = new MathNumber(
						(Double) resolveCostantExpression((ValueExpression) ((BinaryExpression) expression).getLeft()));

				if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
					value = value.subtract(value.multiply(new MathNumber(2)));
				}

			} else {
				value = new MathNumber(offset);
			}

		} catch (MathNumberConversionException ex) {
		}

		for (int i = 0; i < curMatrix.length; i++) {
			for (int j = 0; j < curMatrix.length; j++) {
				if ((i == 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 1) ||
						(j == 2 * newVariableIndex - 1 && i != 2 * newVariableIndex - 2
								&& i != 2 * newVariableIndex - 1)) {
					curMatrix[i][j] = curMatrix[i][j].subtract(value);
				} else if ((i != 2 * newVariableIndex - 2 && i != 2 * newVariableIndex - 1
						&& j == 2 * newVariableIndex - 2) ||
						(j != 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 1
								&& i == 2 * newVariableIndex - 1)) {
					curMatrix[i][j] = curMatrix[i][j].add(value);
				} else if (i == 2 * newVariableIndex - 2 && j == 2 * newVariableIndex - 1) {
					curMatrix[i][j] = curMatrix[i][j].subtract(value.add(value));
				} else if (i == 2 * newVariableIndex - 1 && j == 2 * newVariableIndex - 2) {
					curMatrix[i][j] = curMatrix[i][j].add(value.add(value));
				}
			}
		}
	}

	/**
	 * Handles the first case of assignment: constant assignment like
	 * {@code Vi = c}. This method sets tight bounds for the variable by
	 * encoding the constraint that the variable equals the constant. It applies
	 * strong closure on a copy to compute proper constraint propagation and
	 * then updates the matrix entries for both the positive and negative
	 * occurrences of the variable.
	 * 
	 * @param curMatrix        the current matrix being modified
	 * @param newVariableIndex the index of the variable being assigned
	 * @param expression       the constant expression being assigned
	 * @param id               the identifier being assigned to
	 * @param varIndex         the variable index mapping
	 */
	public void firstCaseAssignement(
			MathNumber[][] curMatrix,
			int newVariableIndex,
			ValueExpression expression,
			Identifier id,
			Map<Identifier, Integer> varIndex) {

		MathNumber[][] copyMatrix = new MathNumber[curMatrix.length][curMatrix.length];

		Floyd.copyArray(copyMatrix, curMatrix);
		Floyd.strongClosureFloyd(copyMatrix);

		MathNumber value = new MathNumber(0);
		try {
			if (expression instanceof BinaryExpression) {
				value = new MathNumber((Double) resolveCostantExpression((BinaryExpression) expression));

				if (((BinaryExpression) expression).getOperator().toString().equals("-")) {
					value = value.subtract(value.multiply(new MathNumber(2)));
				}

			} else {
				value = new MathNumber(Double.parseDouble(expression.toString()));
			}
		} catch (MathNumberConversionException ex) {
		}
		int pos = idToPos(id, varIndex);
		int neg = idToNeg(id, varIndex);
		// ensure indices fit in curMatrix
		if (pos >= curMatrix.length || neg >= curMatrix.length) {
			throw new IllegalStateException("Matrix is too small for identifier positions: pos=" + pos + " neg="
					+ neg + " size=" + curMatrix.length);
		}

		// Add, remove old variable references from V_2i and V_2i-1 to Vi
		// by implementing the operation V <- ?
		for (int i = 0; i < curMatrix.length; i++) {
			for (int j = 0; j < curMatrix.length; j++) {
				if (i == 2 * newVariableIndex - 2 && j == 2 * newVariableIndex - 1) {
					curMatrix[i][j] = value.multiply(new MathNumber(-2));
				} else if (i == 2 * newVariableIndex - 1 && j == 2 * newVariableIndex - 2) {
					curMatrix[i][j] = value.multiply(new MathNumber(2));
				} else {
					try {
						curMatrix[i][j] = (new DifferenceBoundMatrix(copyMatrix, this.variableIndex))
								.forgetIdentifier(id).matrix[i][j];

					} catch (SemanticException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	public double resolveStringMath(
			String expr) {
		if (isDouble(expr)) {
			return Double.parseDouble(expr);
		} else {
			String left = "";
			String center = "";
			String right = "";

			for (int i = 0; i < expr.length(); i++) {
				if (expr.charAt(i) != '(') {
					left += expr.charAt(i);

					if (i == expr.length() - 1) {
						double result = 0;

						// Calculate the accumulated part of left
						if (left.split(Pattern.quote("+")).length > 1) {
							for (int j = 0; j < left.split(Pattern.quote("+")).length; j++) {
								String p = left.split(Pattern.quote("+"))[j];
								result += resolveStringMath(p);
							}

							return result;
						} else if (left.split(Pattern.quote("-")).length > 1) {
							for (int j = 0; j < left.split(Pattern.quote("-")).length; j++) {
								String p = left.split(Pattern.quote("-"))[j];
								result += resolveStringMath(p);
							}
							return result;
						} else if (left.split(Pattern.quote("*")).length > 1) {
							result = 1;
							for (int j = 0; j < left.split(Pattern.quote("*")).length; j++) {
								String p = left.split(Pattern.quote("*"))[j];
								result *= resolveStringMath(p);
							}

							return result;
						} else if (left.split(Pattern.quote("/")).length > 1) {
							String numero = "";

							for (int j = left.split(Pattern.quote("/"))[0].length() - 1; j >= 0; j--) {
								if (expr.charAt(j) == '0' || expr.charAt(j) == '1' || expr.charAt(j) == '2'
										|| expr.charAt(j) == '3' || expr.charAt(j) == '4' || expr.charAt(j) == '5'
										|| expr.charAt(j) == '6' || expr.charAt(j) == '7' || expr.charAt(j) == '8'
										|| expr.charAt(j) == '9') {
									numero += expr.charAt(j);
								} else {
									break;
								}
							}

							result = Double.parseDouble(new StringBuilder(numero).reverse().toString());

							for (int j = 1; j < left.split(Pattern.quote("/")).length; j++) {
								String p = left.split(Pattern.quote("/"))[j];
								result /= resolveStringMath(p);
							}

							return result;
						}

					}
				} else {
					// Find the right parenthesis:
					int indR = 0;
					for (int r = expr.length() - 1; r >= 0; r--) {
						if (expr.charAt(r) == ')') {
							indR = r;
							break;
						} else {
							right += expr.charAt(r);
						}
					}

					for (int k = i + 1; k < indR; k++) {
						center += expr.charAt(k);
					}

					double mid = resolveStringMath(center);

					right = new StringBuilder(right).reverse().toString();

					return resolveStringMath(left + mid + right);
				}
			}
		}

		return 0;
	}

	public boolean isValidConstraint(
			MathNumber[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				int indexVariableI = -1, indexVariableJ = -1;

				for (Identifier key : this.variableIndex.keySet()) {
					if (this.variableIndex.get(key) == i) {
						indexVariableI = i + 1;
					}

					if (this.variableIndex.get(key) == j) {
						indexVariableJ = j + 1;
					}
				}

				if (indexVariableI != -1 && indexVariableJ != -1 && indexVariableI != indexVariableJ) {
					if (matrix[indexVariableJ][indexVariableI].subtract(matrix[indexVariableI][indexVariableJ])
							.compareTo(matrix[indexVariableJ * 2 - 2][indexVariableI * 2 - 2]) > 0 ||
							matrix[indexVariableJ][indexVariableI].subtract(matrix[indexVariableI][indexVariableJ])
									.compareTo(matrix[indexVariableJ * 2 - 1][indexVariableI * 2 - 1]) > 0) {
						return false;
					} else if (matrix[indexVariableJ][indexVariableI].add(matrix[indexVariableI][indexVariableJ])
							.compareTo(matrix[indexVariableJ * 2 - 1][indexVariableI * 2 - 2]) > 0 ||
							matrix[indexVariableJ][indexVariableI].add(matrix[indexVariableI][indexVariableJ])
									.compareTo(matrix[indexVariableJ * 2 - 2][indexVariableI * 2 - 1]) > 0) {
						return false;
					} else if (matrix[indexVariableJ][indexVariableI]
							.subtract(matrix[indexVariableJ][indexVariableI].multiply(new MathNumber(2)))
							.subtract(matrix[indexVariableI][indexVariableJ])
							.compareTo(matrix[indexVariableJ * 2 - 2][indexVariableI * 2 - 1]) > 0 ||
							matrix[indexVariableJ][indexVariableI]
									.subtract(matrix[indexVariableJ][indexVariableI].multiply(new MathNumber(2)))
									.subtract(matrix[indexVariableI][indexVariableJ])
									.compareTo(matrix[indexVariableJ * 2 - 1][indexVariableI * 2 - 2]) > 0) {
						return false;
					}
				} else if (indexVariableI == indexVariableJ && indexVariableI > 0) {
					if (matrix[indexVariableI][indexVariableI].compareTo(
							matrix[indexVariableI * 2 - 1][indexVariableI * 2 - 2].multiply(new MathNumber(2))) > 0) {
						return false;
					} else if (matrix[indexVariableI][indexVariableI].compareTo(
							matrix[indexVariableI * 2 - 2][indexVariableI * 2 - 1].multiply(new MathNumber(-2))) > 0) {
						return false;
					}
				}

			}
		}
		return true;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		DifferenceBoundMatrix that = (DifferenceBoundMatrix) obj;
		return isBottom() == that.isBottom() && isTop() == that.isTop() && Arrays.deepEquals(this.matrix, that.matrix)
				&& Objects.equals(this.variableIndex, that.variableIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.deepHashCode(matrix), variableIndex);
	}

	// Control functions
	boolean verifyDoubleIndex(
			MathNumber[][] mat,
			int indexI,
			int indexJ,
			MathNumber valueI,
			MathNumber valueJ) {
		int I2 = indexI * 2 - 1;
		int I2_Minus1 = indexI * 2 - 2;
		int J2 = indexJ * 2 - 1;
		int J2_Minus1 = indexJ * 2 - 2;

		for (int i = 0; i < mat.length; i++) {
			for (int j = 0; j < mat.length; j++) {
				// First condition
				if (i == I2_Minus1 && j == J2_Minus1) {
					if (valueI.subtract(valueJ).gt(mat[j][i])) {
						return false;
					}
				}

				if (i == I2 && j == J2) {
					if (valueJ.multiply(new MathNumber(-1)).subtract(valueI.multiply(new MathNumber(-1)))
							.gt(mat[i][j])) {
						return false;
					}
				}

				// Second condition
				if (i == I2_Minus1 && j == J2) {
					if (valueI.subtract(valueJ.multiply(new MathNumber(-1))).gt(mat[j][i])) {
						return false;
					}
				}

				if (i == I2 && j == J2_Minus1) {
					if (valueJ.subtract(valueI.multiply(new MathNumber(-1))).gt(mat[i][j])) {
						return false;
					}
				}

				// Third condition
				if (i == I2 && j == J2_Minus1) {
					if (valueI.multiply(new MathNumber(-1)).subtract(valueJ).gt(mat[j][i])) {
						return false;
					}
				}

				if (i == I2_Minus1 && j == J2) {
					if (valueJ.multiply(new MathNumber(-1)).subtract(valueI).gt(mat[i][j])) {
						return false;
					}
				}

			}
		}

		return true;
	}

	boolean verifySingleIndex(
			MathNumber[][] mat,
			int indexI,
			MathNumber valueI) {
		int I2 = indexI * 2 - 1;
		int I2_Minus1 = indexI * 2 - 2;

		for (int i = 0; i < mat.length; i++) {
			for (int j = 0; j < mat.length; j++) {
				// First condition
				if (i == I2_Minus1 && j == I2) {
					if (valueI.subtract(valueI.multiply(new MathNumber(-1)))
							.gt(mat[j][i].multiply(new MathNumber(2)))) {
						return false;
					}
				}

				// Second condition
				if (i == I2 && j == I2_Minus1 && mat[j][i] != MathNumber.PLUS_INFINITY) {
					if (valueI.multiply(new MathNumber(-1)).subtract(valueI)
							.gt(mat[j][i].multiply(new MathNumber(-2)))) {
						return false;
					}
				}

			}
		}

		return true;
	}
}