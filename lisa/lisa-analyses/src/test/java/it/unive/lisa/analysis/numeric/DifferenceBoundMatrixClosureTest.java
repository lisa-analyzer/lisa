package it.unive.lisa.analysis.numeric;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.HashMap;
import org.junit.Test;

/**
 * Tests focused on closure() and strongClosure() transformations.
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */
public class DifferenceBoundMatrixClosureTest {

	private static final double INF = Double.MAX_VALUE;

	private static DifferenceBoundMatrix dbmFrom(
			double[][] vals) {
		int n = vals.length;
		MathNumber[][] matrix = new MathNumber[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double v = vals[i][j];
				if (Double.isInfinite(v) || v == Double.MAX_VALUE)
					matrix[i][j] = MathNumber.PLUS_INFINITY;
				else if (v == 0d)
					matrix[i][j] = MathNumber.ZERO;
				else
					matrix[i][j] = new MathNumber(v);
			}
		}
		return new DifferenceBoundMatrix(matrix, new HashMap<Identifier, Integer>());
	}

	@Test
	// "strongClosure on provided matrix -> expected output"
	public void testStrongClosureProvidedMatrix() throws SemanticException {
		// Input matrix
		DifferenceBoundMatrix in = dbmFrom(new double[][] {
				{ 0, 0, 0, INF },
				{ 2, 0, INF, 0 },
				{ 0, INF, 0, INF },
				{ INF, 0, INF, 0 }
		});

		// Expected matrix after strong closure
		DifferenceBoundMatrix expected = dbmFrom(new double[][] {
				{ 0, 0, 0, 0 },
				{ 2, 0, 2, 0 },
				{ 0, 0, 0, 0 },
				{ 2, 0, 2, 0 }
		});

		DifferenceBoundMatrix out = in.strongClosure();
		assertTrue("strongClosure result does not match the expected matrix", out.equals(expected));
	}

	@Test
	// "closure then strongClosure equals direct strongClosure"
	public void testClosureThenStrongClosureEquivalence() throws SemanticException {
		// Direct strong closure
		DifferenceBoundMatrix directStrong = dbmFrom(new double[][] {
				{ 0, 0, 0, INF },
				{ 2, 0, INF, 0 },
				{ 0, INF, 0, INF },
				{ INF, 0, INF, 0 }
		}).strongClosure();

		// Apply closure first, then strongClosure
		DifferenceBoundMatrix afterClosureThenStrong = dbmFrom(new double[][] {
				{ 0, 0, 0, INF },
				{ 2, 0, INF, 0 },
				{ 0, INF, 0, INF },
				{ INF, 0, INF, 0 }
		}).closure().strongClosure();

		assertTrue("closure() followed by strongClosure() should match direct strongClosure()",
				directStrong.equals(afterClosureThenStrong));
	}

	@Test
	// "closure on invalid matrix throws"
	public void testClosure2() throws SemanticException {
		// Input matrix
		DifferenceBoundMatrix in = dbmFrom(new double[][] {
				{ 0, -10 },
				{ 4, 0 }
		});

		DifferenceBoundMatrix out = in.closure();
		assertTrue("closure on invalid DBM should yield bottom", out.isBottom());
	}

	@Test
	// "strong closure on invalid matrix throws"
	public void testStrongClosure2() throws SemanticException {
		// Input matrix
		DifferenceBoundMatrix in = dbmFrom(new double[][] {
				{ 0, -10 },
				{ 4, 0 }
		});

		DifferenceBoundMatrix out = in.strongClosure();
		assertTrue("strong closure on invalid DBM should yield bottom", out.isBottom());
	}
}
