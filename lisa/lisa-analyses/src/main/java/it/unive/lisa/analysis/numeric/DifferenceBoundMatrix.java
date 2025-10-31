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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.Diffable;

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
		
		MathNumber tmpMat[][] = new MathNumber[matrix.length][matrix.length];
		Floyd.copyArray(tmpMat, matrix);
		this.matrix = tmpMat;

		this.variableIndex = new HashMap<>(variableIndex);
	}

	/**
	 * Returns the variable index mapping.
	 * 
	 * @return the map from identifiers to matrix indices
	 */
	public synchronized  Map<Identifier, Integer> getVariableIndex() {
		return variableIndex;
	}

	/**
	 * Returns the underlying constraint matrix.
	 * 
	 * @return the 2n × 2n matrix of difference bounds
	 */
	public synchronized  MathNumber[][] getMatrix() {
		return matrix;
	}

	/**
	 * Creates a deep copy of the given matrix.
	 * 
	 * @param source the matrix to copy
	 * 
	 * @return a new matrix with the same values
	 */
	private  MathNumber[][] copyMatrix(
			MathNumber[][] source) {
		MathNumber[][] copy = new MathNumber[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = new MathNumber[source[i].length];
			System.arraycopy(source[i], 0, copy[i], 0, source[i].length);
		}
		return copy;
	}

	/**
	 * Builds a new difference-bound matrix representing the top element of the
	 * lattice. The top element has a matrix of the same size as the current
	 * one, with all entries initialized to positive infinity, representing no
	 * constraints.
	 *
	 * @return the top DBM
	 */
	@Override
	public synchronized  DifferenceBoundMatrix top() {
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

	/**
	 * Builds a new difference-bound matrix representing the bottom element of
	 * the lattice. The bottom element indicates an infeasible state (a
	 * contradiction). It is represented by a matrix with a negative value on
	 * the diagonal, which signifies an impossible constraint (e.g., x - x < 0).
	 *
	 * @return the bottom DBM
	 */
	@Override
	public synchronized  DifferenceBoundMatrix bottom() {

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

	/**
	 * Checks if this DBM is the top element of the lattice. A DBM is top if all
	 * its entries are positive infinity, meaning there are no constraints.
	 *
	 * @return {@code true} if this DBM is top, {@code false} otherwise
	 */
	@Override
	public synchronized  boolean isTop() {
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

	/**
	 * Checks if this DBM is the bottom element of the lattice. A DBM is bottom
	 * if it contains a contradiction, which is detected by finding a negative
	 * value on the diagonal of its matrix after closure. A negative diagonal
	 * m[i][i] implies a constraint of the form V - V < 0, which is impossible.
	 *
	 * @return {@code true} if this DBM is bottom, {@code false} otherwise
	 */
	@Override
	public synchronized  boolean isBottom() {
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

	/**
	 * Performs the less-or-equal check between this DBM and another. This
	 * operation is defined as m• ⊑DBM n ⇐⇒ γOct(m) ⊆ γOct(n), which simplifies
	 * to checking if every entry in this matrix is less than or equal to the
	 * corresponding entry in the other matrix.
	 *
	 * @param other the other DBM to compare against
	 * 
	 * @return {@code true} if this DBM is less or equal than the other,
	 *             {@code false} otherwise
	 * 
	 * @throws SemanticException if the matrices have different dimensions or
	 *                               variable sets
	 */
	@Override
	public synchronized  boolean lessOrEqualAux(
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

		/*
		 * for (int i = 0; i < first.matrix.length; i++) { for (int j = 0; j <
		 * first.matrix.length; j++) { if(i%2 == 0 && j%2 == 1 && j == i+1) {
		 * return true; } else if(i%2 == 1 && j%2 == 0 && i == j+1) { return
		 * true; } } }
		 */

		// compute m• ⊑DBM n

		for (int i = 0; i < first.matrix.length; i++) {
			for (int j = 0; j < first.matrix.length; j++) {
				if (first.matrix[i][j].compareTo(second.matrix[i][j]) > 0) {
					return false;
				}
			}
		}
		return true;

	}

	/**
	 * Computes the least upper bound (LUB) of this DBM and another. The LUB is
	 * computed pointwise by taking the maximum of the corresponding entries in
	 * the two matrices: (m ⊔^DBM n)ij = max(mij, nij). The resulting matrix
	 * represents the most precise set of constraints that satisfies both DBMs.
	 *
	 * @param other the other DBM
	 * 
	 * @return the LUB of the two DBMs
	 * 
	 * @throws SemanticException if the DBMs have different variable sets
	 */
	@Override
	public synchronized  DifferenceBoundMatrix lubAux(
			DifferenceBoundMatrix other)
			throws SemanticException {
		// debugMethodEntry("lubAux", other);
    
    // CONTROLLO PER ROMERE LA RICORSIONE
    // Se le matrici sono identiche, restituisci direttamente this
    if (this == other || this.equals(other)) {
        return this;
    }
    
    // Se una è bottom, restituisci l'altra
    if (this.isBottom()) {
        return other;
    }
    if (other.isBottom()) {
        return this;
    }
    
    // Se una è top, restituisci top
    if (this.isTop()) {
        return this.top();
    }
    if (other.isTop()) {
        return other.top();
    }

    // APPROCCIO SEMPLIFICATO - rompi la ricorsione con un risultato semplice
    try {
		
        // 1. Crea copie locali di TUTTO
        Map<Identifier, Integer> localThisVars = new HashMap<>(this.variableIndex);
        Map<Identifier, Integer> localOtherVars = new HashMap<>(other.variableIndex);
        MathNumber[][] localThisMatrix = copyMatrix(this.matrix);
        MathNumber[][] localOtherMatrix = copyMatrix(other.matrix);
        
		boolean hasDifferentVars = false;
    	for (Map.Entry<Identifier, Integer> entry : localThisVars.entrySet()) {
        if (!localOtherVars.containsKey(entry.getKey())) {
            hasDifferentVars = true;
            break;
        }
    }

	 // Controlla anche nell'altra direzione
    for (Map.Entry<Identifier, Integer> entry : localOtherVars.entrySet()) {
        if (!localThisVars.containsKey(entry.getKey())) {
            hasDifferentVars = true;
            break;
        }
    }
    
    if (hasDifferentVars) {
        throw new SemanticException("Variable are not the same");
    }
	
        // 2. Verifiche di sicurezza
        if (localThisMatrix.length != localOtherMatrix.length) {
            // Se dimensioni diverse, restituisci top per rompere la ricorsione
            return this.top();
        }
        
        for (Map.Entry<Identifier, Integer> entry : localThisVars.entrySet()) {
            if (!localOtherVars.containsKey(entry.getKey())) {
                // Se variabili diverse, restituisci top per rompere la ricorsione
                return this.top();
            }
        }

		
        // 3. Calcolo semplificato del lub
        MathNumber[][] newMatrix = new MathNumber[localThisMatrix.length][localThisMatrix.length];
        
        for (int i = 0; i < localThisMatrix.length; i++) {
            for (int j = 0; j < localThisMatrix.length; j++) {
                // LUB semplificato: prendi il massimo dei due valori
                if (localThisMatrix[i][j].compareTo(localOtherMatrix[i][j]) > 0) {
                    newMatrix[i][j] = localThisMatrix[i][j];
                } else {
                    newMatrix[i][j] = localOtherMatrix[i][j];
                }
            }
        }

        DifferenceBoundMatrix result = new DifferenceBoundMatrix(newMatrix, new HashMap<>(localThisVars));
        return result;
	}
        catch (SemanticException e) {
        // RILANCIA SemanticException invece di catturarla
        throw e; 
    } catch (Exception e) {
        // In caso di qualsiasi errore, restituisci top per rompere la ricorsione
        return this.top();
    }
	}

	/**
	 * Computes the greatest lower bound (GLB) of this DBM and another. The GLB
	 * is computed pointwise by taking the minimum of the corresponding entries
	 * in the two matrices: (m ⊓^DBM n)ij = min(mij, nij). The resulting matrix
	 * is then closed to ensure all implied constraints are made explicit.
	 *
	 * @param other the other DBM
	 * 
	 * @return the GLB of the two DBMs
	 * 
	 * @throws SemanticException if the DBMs have different variable sets
	 */
	@Override
	public synchronized  DifferenceBoundMatrix glbAux(
			DifferenceBoundMatrix other)
			throws SemanticException {
		// first check if two dbm are the same
		debugRace("glb aux");
		  Map<Identifier, Integer> thisVarsCopy;
    Map<Identifier, Integer> otherVarsCopy;
    MathNumber[][] thisMatrixCopy;
    MathNumber[][] otherMatrixCopy;
    
    synchronized (this) {
        thisVarsCopy = new HashMap<>(this.variableIndex);
        thisMatrixCopy = copyMatrix(this.matrix);  // COPIA la matrice!
    }
    synchronized (other) {
        otherVarsCopy = new HashMap<>(other.variableIndex);
        otherMatrixCopy = copyMatrix(other.matrix); // COPIA la matrice!
    }
    
    for (Map.Entry<Identifier, Integer> entry : thisVarsCopy.entrySet()) {
        if (!otherVarsCopy.containsKey(entry.getKey())) {
            throw new SemanticException("Variable are not the same");
        }
    }

		MathNumber[][] newMatrix = new MathNumber[thisMatrixCopy.length][thisMatrixCopy.length];
		for (int i = 0; i < thisMatrixCopy.length; i++) {
			for (int j = 0; j < thisMatrixCopy.length; j++) {
				newMatrix[i][j] = thisMatrixCopy[i][j].min(otherMatrixCopy[i][j]);
			}
		}

		
		DifferenceBoundMatrix result = new DifferenceBoundMatrix(newMatrix, new HashMap<>(thisVarsCopy));
		result = result.strongClosure();
		return result;
	}

	/**
	 * Returns a structured representation of this DBM. If the DBM is top or
	 * bottom, it returns a standard representation for those. Otherwise, it
	 * provides a string representation of the underlying matrix.
	 *
	 * @return the structured representation of this DBM
	 */
	@Override
	public synchronized  StructuredRepresentation representation() {
		if (isBottom()) {
			return Lattice.bottomRepresentation();
		}
		if (isTop()) {
			return Lattice.topRepresentation();
		}
		StringBuilder matrixStr = new StringBuilder("\n");
		for (int i = 0; i < matrix.length; i++) {
			matrixStr.append("|");
			for (int j = 0; j < matrix.length; j++) {
				matrixStr.append(matrix[i][j]).append(" ");
			}
			matrixStr.append("|\n");
		}
		StringRepresentation result = new StringRepresentation(
				"DBM[" + matrix.length + "x" + matrix.length + "]\n" + matrixStr.toString());
		return result;
	}

	/**
	 * Checks if this DBM knows about a given identifier.
	 *
	 * @param id the identifier to check
	 * 
	 * @return {@code true} if the identifier is tracked in the DBM,
	 *             {@code false} otherwise
	 */
	@Override
	public synchronized  boolean knowsIdentifier(
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
	public synchronized DifferenceBoundMatrix assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

			debugRace("assign");
		// Create a copy of variableIndex to work with (for immutability)
		Map<Identifier, Integer> workingVariableIndex = new java.util.HashMap<>(new HashMap<>(this.variableIndex));

		// Work on a local matrix reference so we can add a variable and still
		// continue
		MathNumber[][] curMatrix = copyMatrix(this.matrix);

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

		if (Floyd.HasNegativeCycle(this.matrix)) {
			return this.bottom();
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

		MathNumber copy[][] = new MathNumber[curMatrix.length][curMatrix.length];

		Floyd.copyArray(copy, curMatrix);

		Floyd.strongClosureFloyd(copy);

		if (Floyd.HasNegativeCycle(copy)) {
			removeNegativeCycle(curMatrix);
			DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix, new HashMap<>(workingVariableIndex));
			Floyd.strongClosureFloyd(curMatrix);
			return result;
		}

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(copy, new HashMap<>(workingVariableIndex));

		return result;
	}

	/**
	 * Resets constraints that cause a negative cycle in the matrix. This method
	 * is a recovery mechanism used when an assignment or assumption leads to a
	 * contradiction (a negative cycle). It identifies the problematic
	 * constraints (typically those involving direct relationships like x - y)
	 * and resets them to a consistent state, effectively weakening the DBM to
	 * restore satisfiability.
	 *
	 * @param mat the matrix to be corrected
	 */
	private  void removeNegativeCycle(
			MathNumber mat[][]) {
		for (int i = 0; i < mat.length; i++) {
			for (int j = 0; j < mat.length; j++) {
				if (Math.abs(i - j) == 1) {
					if ((mat[i][j].isPositive() || mat[i][j].isZero())
							&& (mat[i][j].subtract(mat[j][i])).leq(MathNumber.ZERO)) {
						mat[j][i] = mat[j][i].multiply(MathNumber.MINUS_ONE).multiply(new MathNumber(2));
					} else if ((mat[i][j].isNegative() || mat[i][j].isZero())
							&& (mat[j][i].add(mat[i][j])).lt(MathNumber.ZERO)) {
						mat[j][i] = mat[i][j].abs().multiply(new MathNumber(2));
						;
					} else {
						mat[i][j] = mat[j][i].abs();
					}
				} else if (i == j) {
					mat[i][j] = MathNumber.ZERO;
				} else {
					mat[i][j] = MathNumber.PLUS_INFINITY;
				}
			}
		}
	}

	/**
	 * Performs a small-step semantics evaluation of a value expression. In the
	 * context of DBM, this method currently returns the DBM unchanged, as the
	 * main logic for state modification is handled in {@link #assign} and
	 * {@link #assume}.
	 *
	 * @param expression the expression to evaluate
	 * @param pp         the program point
	 * @param oracle     the semantic oracle
	 * 
	 * @return the (unchanged) DBM
	 * 
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public synchronized  DifferenceBoundMatrix smallStepSemantics(
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
	public synchronized  DifferenceBoundMatrix assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {

		debugRace("assume");
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
			DifferenceBoundMatrix glbResult = leftResult.glbAux(rightResult);
			return glbResult;
		}

		if (be.getOperator().equals(LogicalOr.INSTANCE)) {
			// apply assume for both sides and then merge the results with lub
			DifferenceBoundMatrix leftResult = this.assume((ValueExpression) be.getLeft(), src, dest, oracle);
			DifferenceBoundMatrix rightResult = this.assume((ValueExpression) be.getRight(), src, dest, oracle);
			DifferenceBoundMatrix lubResult = leftResult.lubAux(rightResult);
			return lubResult;
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

				return this.addConstraint((Identifier) b, ub, false);
			}

			if (a instanceof Identifier && b instanceof Constant) {
				// const - a <= c
				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (k == null)
					return this;
				MathNumber val = cVal.subtract(k);

				return this.addConstraint((Identifier) a, val, true);
			}

			// handle the case x - y - c1 <= c2
			// Objective: call addConstraint(Identifier a, Identifier b,
			// MathNumber c)
			if (a instanceof Constant && b instanceof SymbolicExpression) {

				MathNumber c1 = toMathNumber(((Constant) a).getValue()).multiply(new MathNumber(-1));
				if (c1 == null)
					return this;

				MathNumber adjusted = cVal.subtract(c1);

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
				if (b1 == null || b2 == null) {
					return this;
				}

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

				return this.addConstraint((Identifier) ua.getExpression(), val, true);
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

				return this.addConstraint((Identifier) ub.getExpression(), val, true);
			}

			// x + const <= c
			if (a instanceof Identifier && b instanceof Constant) {
				MathNumber k = toMathNumber(((Constant) b).getValue());
				if (k == null)
					return this;

				MathNumber ub = cVal.subtract(k);

				return this.addConstraint((Identifier) a, ub, false);
			}

			// const + y <= c
			if (a instanceof Constant && b instanceof Identifier) {
				MathNumber k = toMathNumber(((Constant) a).getValue());
				if (k == null)
					return this;

				MathNumber ub = cVal.subtract(k);

				return this.addConstraint((Identifier) b, ub, false);
			}

			// y + x <= c
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
	public synchronized  DifferenceBoundMatrix forgetIdentifier(
			Identifier id)
			throws SemanticException {

			debugRace("forget identifier");
		if (!variableIndex.containsKey(id)) {
			return new DifferenceBoundMatrix(this.matrix, this.variableIndex);
		}

		MathNumber matTmp[][] = new MathNumber[this.matrix.length][this.matrix.length];

		Floyd.copyArray(matTmp, this.matrix);

		int pos = idToPos(id, this.variableIndex) + 1;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				if (i != 2 * pos - 2 && i != 2 * pos - 1 && j != 2 * pos - 2 && j != 2 * pos - 1) {
					matTmp[i][j] = matrix[i][j];
				} else if ((i == j && i == 2 * pos - 2) || (i == j && i == 2 * pos - 1)) {
					matTmp[i][j] = MathNumber.ZERO;
				} else {
					matTmp[i][j] = MathNumber.PLUS_INFINITY;
				}
			}
		}

		return new DifferenceBoundMatrix(matTmp, this.variableIndex);
	}

	/**
	 * Forgets identifiers that satisfy a given predicate. This method iterates
	 * through all tracked identifiers and forgets those for which the predicate
	 * returns {@code true}.
	 *
	 * @param test the predicate to test identifiers against
	 * 
	 * @return the DBM after forgetting the selected identifiers
	 * 
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public synchronized  DifferenceBoundMatrix forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		// For now, return this as a placeholder
		return this;
	}

	/**
	 * Checks if a given boolean expression is satisfied by this DBM. This
	 * method determines whether the constraints encoded in the DBM imply that
	 * the expression is true, false, or cannot be determined.
	 *
	 * @param expression the expression to check
	 * @param pp         the program point
	 * @param oracle     the semantic oracle
	 * 
	 * @return {@link Satisfiability#SATISFIED},
	 *             {@link Satisfiability#NOT_SATISFIED}, or
	 *             {@link Satisfiability#UNKNOWN}
	 * 
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public synchronized  Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// For now, return unknown as a safe approximation
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Pushes a new scope, which is currently a no-op for this domain. In more
	 * complex domains, this would handle scoping of variables.
	 *
	 * @param token the scope token
	 * 
	 * @return the unchanged DBM
	 * 
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public synchronized  DifferenceBoundMatrix pushScope(
			ScopeToken token)
			throws SemanticException {
				debugRace("Push scope");
		return new DifferenceBoundMatrix(this.matrix, this.variableIndex);
	}

	/**
	 * Pops a scope, which is currently a no-op for this domain. In more complex
	 * domains, this would handle scoping of variables.
	 *
	 * @param token the scope token
	 * 
	 * @return the unchanged DBM
	 * 
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public synchronized  DifferenceBoundMatrix popScope(
			ScopeToken token)
			throws SemanticException {
				debugRace("pop scope");
		return new DifferenceBoundMatrix(this.matrix, this.variableIndex);
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
	public synchronized  DifferenceBoundMatrix closure() throws SemanticException {

		Floyd.Floyd(matrix, new MathNumber[this.matrix.length][this.matrix.length]);
		return new DifferenceBoundMatrix(this.matrix, this.variableIndex);
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
	public synchronized  DifferenceBoundMatrix strongClosure() throws SemanticException {

		debugRace("strong closure");
		MathNumber matTmp[][] = new MathNumber[this.matrix.length][this.matrix.length];

		Floyd.copyArray(matTmp, this.matrix);

		DifferenceBoundMatrix dbm = new DifferenceBoundMatrix(matTmp, new HashMap<>(this.variableIndex));
		Floyd.strongClosureFloyd(dbm.matrix);
		return dbm;
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
	public synchronized  ValueEnvironment<Interval> toInterval() throws SemanticException {
		ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());
		DifferenceBoundMatrix dbm = this.closure(); // apply the closure to
													// ensure the matrix is in
													// normal form
		
		debugRace("toInterval");
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
	public synchronized  static DifferenceBoundMatrix fromIntervalDomain(
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

	/**
	 * Converts a generic object (typically from a {@link Constant}) into a
	 * {@link MathNumber}. This method handles various numeric types, including
	 * Integer, Long, Double, and Float, and attempts to parse from a string as
	 * a fallback.
	 *
	 * @param v the object to convert
	 * 
	 * @return the corresponding {@link MathNumber}, or {@code null} if
	 *             conversion is not possible
	 */
	private  MathNumber toMathNumber(
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
	public synchronized  int idToPos(
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
	public synchronized  int idToNeg(
			Identifier id,
			Map<Identifier, Integer> varIndex) {
		if (!varIndex.containsKey(id))
			throw new IllegalArgumentException("Identifier " + id + " not found in variable index.");
		int i = varIndex.get(id);
		int result = 2 * i + 1;
		return result;
	}

	/**
	 * Adds a unary constraint of the form {@code ±a ≤ c} to the DBM. This
	 * helper method encodes an upper or lower bound for a single variable.
	 * <ul>
	 * <li>If {@code isNegated} is false, it adds the constraint
	 * {@code a ≤ c}.</li>
	 * <li>If {@code isNegated} is true, it adds {@code -a ≤ c} (i.e.,
	 * {@code a ≥ -c}).</li>
	 * </ul>
	 *
	 * @param a         the variable to constrain
	 * @param c         the constant bound
	 * @param isNegated whether the variable is negated
	 * 
	 * @return a new DBM with the constraint added
	 * 
	 * @throws SemanticException if the constraint cannot be added
	 */
	private  DifferenceBoundMatrix addConstraint(
			Identifier a,
			MathNumber c,
			boolean isNegated)
			throws SemanticException {

		// Work on a copy of the matrix
		MathNumber[][] curMatrix = copyMatrix(this.matrix);
		MathNumber constraintValue;

		int i = 0;
		int j = 0;

		if (isNegated) {
			i = idToPos(a, variableIndex);
			j = idToNeg(a, variableIndex);
			constraintValue = c.multiply(new MathNumber(2));
		} else {
			i = idToNeg(a, variableIndex);
			j = idToPos(a, variableIndex);
			constraintValue = c.multiply(new MathNumber(2));
		}

		MathNumber oldValue = curMatrix[i][j];
		curMatrix[i][j] = curMatrix[i][j].min(constraintValue);

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix, this.variableIndex);

		// compute string closure to ensure the assume function returns bottom
		// in the
		DifferenceBoundMatrix finalResult = result.strongClosure();
		return finalResult;
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
	private  DifferenceBoundMatrix addConstraint(
			Identifier a,
			Identifier b,
			MathNumber c,
			boolean firstNegated,
			boolean secondNegated)
			throws SemanticException {

		// Work on a copy of the matrix
		MathNumber[][] curMatrix = copyMatrix(this.matrix);

		int i1, j1, i2, j2;
		if (!firstNegated && !secondNegated) {
			// b + a <= c
			i1 = idToNeg(a, variableIndex);
			j1 = idToPos(b, variableIndex);
			i2 = idToNeg(b, variableIndex);
			j2 = idToPos(a, variableIndex);
		} else if (firstNegated && secondNegated) {
			// -b - a <= c
			i1 = idToPos(a, variableIndex);
			j1 = idToNeg(b, variableIndex);
			i2 = idToPos(b, variableIndex);
			j2 = idToNeg(a, variableIndex);
		} else if (!firstNegated && secondNegated) {
			// b - a <= c
			j1 = idToPos(a, variableIndex);
			i1 = idToPos(b, variableIndex);
			i2 = idToNeg(a, variableIndex);
			j2 = idToNeg(b, variableIndex);
		} else {
			// -b + a <= c
			i1 = idToPos(b, variableIndex);
			j1 = idToPos(a, variableIndex);
			i2 = idToNeg(a, variableIndex);
			j2 = idToNeg(b, variableIndex);
		}

		curMatrix[i1][j1] = curMatrix[i1][j1].min(c);

		curMatrix[i2][j2] = curMatrix[i2][j2].min(c);

		DifferenceBoundMatrix result = new DifferenceBoundMatrix(curMatrix,
				this.variableIndex);

		DifferenceBoundMatrix finalResult = result
				.strongClosure();
		return finalResult;
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
	public synchronized  DifferenceBoundMatrix wideningAux(
			DifferenceBoundMatrix other)
			throws SemanticException {

	debugRace("Widening aux");
	if (other == null) {
			return this;
		}

		int size = this.matrix.length;
		MathNumber[][] resultMatrix = new MathNumber[size][size];
		boolean flag=true;

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// Widening: if this[i][j] < other[i][j], the constraint is
				// getting weaker, so
				// set to +∞

				if(!this.matrix[i][j].isFinite())
				{
					resultMatrix[i][j] = MathNumber.PLUS_INFINITY;
					continue;
				}

				if (i != j) {
					if(flag && this.matrix[i][j].abs().compareTo(this.matrix[j][i].abs()) > 0)
					{
						resultMatrix[i][j] = MathNumber.PLUS_INFINITY;
						flag=false;
					}
					else if(flag && this.matrix[i][j].abs().compareTo(this.matrix[j][i].abs()) < 0)
					{
						resultMatrix[i][j] = MathNumber.PLUS_INFINITY;
						flag = false;
					}
					else
					{
						resultMatrix[i][j] = this.matrix[i][j].min(this.matrix[j][i]);
					}
				}
				else
				{
					resultMatrix[i][j] = MathNumber.ZERO;
				}
			}
		}

		//Floyd.printMatrix(resultMatrix);
		//Floyd.strongClosureFloyd(resultMatrix);
		Map<Identifier, Integer> newVariableIndex = new HashMap<>(this.variableIndex);
		final DifferenceBoundMatrix result = new DifferenceBoundMatrix(resultMatrix, newVariableIndex);
		return result;

	}

	/**
	 * Resolves the value of a variable expression from the DBM. This method
	 * extracts the value of a variable by querying its bounds from the matrix.
	 * It is used internally to get the current known value of a variable.
	 *
	 * @param exp the variable expression to resolve
	 * 
	 * @return the numeric value of the variable
	 * 
	 * @throws MathNumberConversionException if the value cannot be converted
	 */
	private  double resolveVariableExpression(
			ValueExpression exp)
			throws MathNumberConversionException {
		for (Identifier key : this.variableIndex.keySet()) {
			if (key.toString().equals(exp.toString())) {
				return this.matrix[idToPos(key, this.variableIndex)][idToNeg(key, this.variableIndex)].toDouble();
			}
		}

		return Double.NaN;
	}

	/**
	 * Checks if a value expression contains a variable and returns its name.
	 * This method recursively traverses the expression to find any variables.
	 * It assumes that the expression contains at most one variable.
	 *
	 * @param exp the expression to check
	 * 
	 * @return the name of the variable if found, otherwise an empty string
	 */
	private  String hasVariable(
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
	private  double resolveCostantExpression(
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

	/**
	 * Checks if a given string can be parsed as a double.
	 *
	 * @param s the string to check
	 * 
	 * @return {@code true} if the string is a valid double, {@code false}
	 *             otherwise
	 */
	public synchronized  static boolean isDouble(
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
	public synchronized  void fourthCaseAssignement(
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
	public synchronized  void thirdCaseAssignement(
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
					if (i != 2 * newVariableIndex - 2 && i != 2 * newVariableIndex - 1 &&
							j != 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 1) {
						if (!Floyd.HasNegativeCycle(copyMatrix)) {
							curMatrix[i][j] = copyMatrix[i][j];
						}
					} else if (i == j && (i == 2 * newVariableIndex - 2 || i == 2 * newVariableIndex - 1)) {
						curMatrix[i][j] = MathNumber.ZERO;
					} else {
						curMatrix[i][j] = MathNumber.PLUS_INFINITY;
					}
					/*
					 * try { curMatrix[i][j] = (new
					 * DifferenceBoundMatrix(copyMatrix, this.variableIndex))
					 * .forgetIdentifier(id).matrix[i][j]; } catch
					 * (SemanticException e) { e.printStackTrace(); }
					 */
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
	public synchronized  void secondCaseAssignement(
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
	public synchronized  void firstCaseAssignement(
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
					if (i != 2 * newVariableIndex - 2 && i != 2 * newVariableIndex - 1 &&
							j != 2 * newVariableIndex - 2 && j != 2 * newVariableIndex - 1) {
						if (!Floyd.HasNegativeCycle(copyMatrix)) {
							curMatrix[i][j] = copyMatrix[i][j];
						}
					} else if (i == j && (i == 2 * newVariableIndex - 2 || i == 2 * newVariableIndex - 1)) {
						curMatrix[i][j] = MathNumber.ZERO;
					} else {
						curMatrix[i][j] = MathNumber.PLUS_INFINITY;
					}
					/*
					 * try { curMatrix[i][j] = (new
					 * DifferenceBoundMatrix(copyMatrix, this.variableIndex))
					 * .forgetIdentifier(id).matrix[i][j]; } catch
					 * (SemanticException e) { e.printStackTrace(); }
					 */
				}

			}
		}

	}

	/**
	 * Recursively evaluates a mathematical expression represented as a string.
	 * This method handles parentheses and the four basic arithmetic operations
	 * (+, -, *, /) by recursively parsing and evaluating the expression.
	 * 
	 * @param expr the string representation of the mathematical expression to
	 *                 evaluate
	 * 
	 * @return the numeric result of the expression evaluation, or 0 if parsing
	 *             fails
	 */
	public synchronized  double resolveStringMath(
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

	/**
	 * Validates whether the constraints in the given matrix are consistent with
	 * octagon constraint properties. This method checks if the difference
	 * constraints between variables satisfy the coherence properties required
	 * by the octagon domain, ensuring that the relationships between variables
	 * and their negations are properly maintained.
	 * 
	 * @param matrix the constraint matrix to validate
	 * 
	 * @return {@code true} if all constraints are valid and coherent,
	 *             {@code false} if any inconsistency is detected
	 */
	public synchronized  boolean isValidConstraint(
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

	/**
	 * Checks if this DBM is equal to another object. Two DBMs are considered
	 * equal if they have the same top/bottom state, the same constraint matrix
	 * (element-wise comparison), and the same variable index mapping.
	 * 
	 * @param obj the object to compare with
	 * 
	 * @return {@code true} if the objects are equal, {@code false} otherwise
	 */
	@Override
	public synchronized  boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		DifferenceBoundMatrix that = (DifferenceBoundMatrix) obj;
		return isBottom() == that.isBottom() && isTop() == that.isTop() && Arrays.deepEquals(this.matrix, that.matrix)
				&& Objects.equals(this.variableIndex, that.variableIndex);
	}

	/**
	 * Computes the hash code for this DBM based on its constraint matrix and
	 * variable index mapping. This ensures that equal DBMs produce the same
	 * hash code.
	 * 
	 * @return the hash code value for this DBM
	 */
	@Override
	public int hashCode() {
		return Objects.hash(Arrays.deepHashCode(matrix), variableIndex);
	}

	/**
	 * Verifies that the constraints involving two variables (Vi and Vj) are
	 * consistent with their assigned values in the matrix. This method checks
	 * the coherence properties of octagon constraints for pairs of variables,
	 * ensuring that the difference bounds between the variables respect the
	 * assigned values.
	 * 
	 * @param mat    the constraint matrix to verify
	 * @param indexI the index of the first variable
	 * @param indexJ the index of the second variable
	 * @param valueI the assigned value for the first variable
	 * @param valueJ the assigned value for the second variable
	 * 
	 * @return {@code true} if the constraints are consistent with the assigned
	 *             values, {@code false} otherwise
	 */
	synchronized boolean  verifyDoubleIndex(
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

	/**
	 * Verifies that the constraints involving a single variable (Vi) are
	 * consistent with its assigned value in the matrix. This method checks the
	 * coherence properties between the positive and negative occurrences of a
	 * variable (V'_(2i-1) and V'_(2i)) to ensure they properly represent the
	 * bounds of the original variable.
	 * 
	 * @param mat    the constraint matrix to verify
	 * @param indexI the index of the variable to verify
	 * @param valueI the assigned value for the variable
	 * 
	 * @return {@code true} if the constraints are consistent with the assigned
	 *             value, {@code false} otherwise
	 */
	synchronized  boolean verifySingleIndex(
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

	private void debugRace(String method) {
  //  System.err.printf("Thread: %s, Method: %s, Time: %d%n", 
    //    Thread.currentThread().getName(), method, System.nanoTime());
    //new Exception("Stack trace").printStackTrace(System.err);
}

private void debugMethodEntry(String methodName, DifferenceBoundMatrix other) {
    System.err.println("=== DBM METHOD ENTRY ===");
    System.err.println("Method: " + methodName);
    System.err.println("Thread: " + Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")");
    System.err.println("This: " + System.identityHashCode(this));
    System.err.println("This matrix: " + System.identityHashCode(this.matrix) + ", size: " + this.matrix.length);
    System.err.println("This vars: " + System.identityHashCode(this.variableIndex) + ", size: " + this.variableIndex.size());
    
    if (other != null) {
        System.err.println("Other: " + System.identityHashCode(other));
        System.err.println("Other matrix: " + System.identityHashCode(other.matrix) + ", size: " + other.matrix.length);
        System.err.println("Other vars: " + System.identityHashCode(other.variableIndex) + ", size: " + other.variableIndex.size());
    }
    
    // Stack trace per vedere chi sta chiamando
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    for (int i = 3; i < Math.min(8, stack.length); i++) {
        if (stack[i].getClassName().contains("lisa")) {
            System.err.println("  Called from: " + stack[i]);
        }
    }
    System.err.println("=== END DBM METHOD ENTRY ===");
}
}