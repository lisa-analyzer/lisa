package it.unive.lisa.util.octagon;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;

/**
 * Utility class providing implementations of the Floyd-Warshall algorithm and
 * its variants for computing transitive closures and shortest paths in weighted
 * graphs. These algorithms are fundamental for octagon domain analysis, where
 * they are used to compute the strong closure of difference-bound matrices.
 * <p>
 * The class provides several variants:
 * <ul>
 * <li>{@link #Floyd(MathNumber[][], MathNumber[][])}: Standard Floyd-Warshall
 * algorithm computing all-pairs shortest paths</li>
 * <li>{@link #WarshallFloyd(MathNumber[][], MathNumber[][])}: Custom variant
 * optimized for incremental updates</li>
 * <li>{@link #strongClosureFloyd(MathNumber[][])}: Specialized variant for
 * octagon strong closure computation</li>
 * </ul>
 * </p>
 * <p>
 * Additionally, the class provides utility methods for negative cycle detection
 * using BFS/DFS traversal, which is crucial for determining if an octagon
 * constraint system is inconsistent (bottom).
 * </p>
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso</a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */

public class Floyd {

	/**
	 * Represents infinity for integer-based calculations (half of
	 * Integer.MAX_VALUE to avoid overflow).
	 */
	public static final int INF = Integer.MAX_VALUE / 2;

	/**
	 * Number of vertices for testing purposes.
	 */
	public static final int V = 6;

	public static void TestNormalFloyd(
			MathNumber[][] mat)
			throws MathNumberConversionException {
		// generateMatrix(dist);

		System.out.println("Negative cycles BFS: " + HasNegativeCycle(mat));

		System.out.println("Before the operation");
		printMatrix(mat);

		System.out.println("Starting floyd calculation");
		MathNumber[][] path = new MathNumber[mat.length][mat.length];

		Floyd(mat, path);

		System.out.println("After the operation for normal Floyd");
		printMatrix(mat);
		System.out.println("Paths matrix for normal Floyd: ");
		printMatrix(path);

	}

	public static void TestCustomFloyd(
			MathNumber[][] mat)
			throws MathNumberConversionException {
		// generateMatrix(dist);

		System.out.println("Negative cycles BFS: " + HasNegativeCycle(mat));

		System.out.println("Before the operation");
		printMatrix(mat);

		System.out.println("Starting custom floyd calculation");
		MathNumber[][] path = new MathNumber[mat.length][mat.length];

		WarshallFloyd(mat, path);

		System.out.println("After the operation for custom Floyd");
		printMatrix(mat);
		System.out.println("Paths matrix for custom Floyd: ");
		printMatrix(path);

	}

	public static void TestStrongClosureFloyd(
			MathNumber[][] mat)
			throws MathNumberConversionException {
		// generateMatrix(dist);

		System.out.println("Negative cycles BFS: " + HasNegativeCycle(mat));

		System.out.println("Before the operation");
		printMatrix(mat);

		System.out.println("Starting strong Floyd calculation");

		strongClosureFloyd(mat);

		System.out.println("After the operation for strong Floyd");
		printMatrix(mat);

	}

	public static boolean equalMatrix(
			MathNumber[][] m1,
			MathNumber[][] m2) {
		for (int y = 0; y < m1.length; y++) {
			for (int x = 0; x < m1.length; x++) {
				if (m1[y][x] != m2[y][x]) {
					System.out.println("Error, they are different on: " + y + ":" + x);
					return false;
				}
			}
		}

		return true;
	}

	public static void initMem(
			MathNumber[][] matrix) {
		int V = matrix.length;

		for (int i = 0; i < V; i++) {
			for (int j = 0; j < V; j++) {
				matrix[i][j] = MathNumber.PLUS_INFINITY;
			}
		}
	}

	public static void copyArray(
			MathNumber[][] matrix1,
			MathNumber[][] matrix2) {
		int V = matrix1.length;

		for (int i = 0; i < V; i++) {
			for (int j = 0; j < V; j++) {
				matrix1[i][j] = matrix2[i][j];
			}
		}
	}

	public static void generateMatrix(
			MathNumber[][] matrix) {
		int V = matrix.length;
		Random rand = new Random();

		for (int i = 0; i < V; i++) {
			for (int j = 0; j < V; j++) {
				if (i != j) {
					if (rand.nextInt(100) < 75) {
						matrix[i][j] = new MathNumber(rand.nextInt(1000));
					} else {
						matrix[i][j] = MathNumber.PLUS_INFINITY;
					}
				}
			}
		}
	}

	public static void printMatrix(
			MathNumber[][] matrix) {
		int V = matrix.length;

		for (int i = 0; i < V; i++) {
			for (int j = 0; j < V; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println("");
		}
	}

	/**
	 * Implements the standard Floyd-Warshall algorithm for computing all-pairs
	 * shortest paths in a weighted graph. The algorithm updates the input
	 * matrix in place to contain the shortest path distances, and records the
	 * path information in the path matrix.
	 * <p>
	 * Time complexity: O(n³) where n is the number of vertices.
	 * </p>
	 * 
	 * @param matrix the distance matrix to be updated with shortest paths
	 *                   (modified in place)
	 * @param path   the path matrix recording intermediate vertices for path
	 *                   reconstruction (modified in place)
	 */
	public static void Floyd(
			MathNumber[][] matrix,
			MathNumber path[][]) {
		int V = matrix.length;

		for (int k = 0; k < V; k++) {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < V; j++) {
					if (matrix[i][k] != MathNumber.PLUS_INFINITY && matrix[k][j] != MathNumber.PLUS_INFINITY) {

						if (matrix[i][j].compareTo(matrix[i][k].add(matrix[k][j])) > 0) {
							matrix[i][j] = matrix[i][k].add(matrix[k][j]);
							path[i][j] = new MathNumber(k);
						} else {
							path[i][j] = new MathNumber(j);
						}
						matrix[i][j] = matrix[i][j].min(matrix[i][k].add(matrix[k][j]));
					}
				}
			}
		}
	}

	/**
	 * Implements a custom variant of the Floyd-Warshall algorithm optimized for
	 * incremental updates. This version uses memoization to track which edges
	 * have been processed and performs two passes of incremental shortest path
	 * computation.
	 * <p>
	 * Unlike the standard algorithm, this variant computes shortest paths
	 * incrementally from each vertex, using recursive calls to ensure all
	 * necessary neighbors are updated before computing a new shortest path.
	 * </p>
	 * 
	 * @param matrix the distance matrix to be updated (modified in place)
	 * @param path   the path matrix for reconstruction (modified in place)
	 */
	public static void WarshallFloyd(
			MathNumber[][] matrix,
			MathNumber[][] path) {
		MathNumber[][] mem = new MathNumber[matrix.length][matrix.length];

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				path[i][j] = MathNumber.MINUS_ONE;
			}
		}

		initMem(mem);
		for (int i = 0; i < matrix.length; i++) {
			Floyd2(matrix, i, matrix.length, mem, path);
		}

		for (int i = 0; i < matrix.length; i++) {
			Floyd2(matrix, i, matrix.length, mem, path);
		}
	}

	public static void Floyd2(
			MathNumber[][] matrix,
			int start,
			int n,
			MathNumber[][] mem,
			MathNumber[][] path) {
		if (n > 0) {
			int V = matrix.length;
			for (int j = 0; j < V; j++) {
				for (int k = 0; k < V; k++) {
					if (mem[k][j].compareTo(matrix[k][j]) > 0 && matrix[k][j] != MathNumber.PLUS_INFINITY) {
						// To use neighbor "k", I need by inductive hypothesis
						// that "k" and all its
						// neighbors are updated
						Floyd2(matrix, k, n - 1, mem, path);

						if (matrix[start][j].compareTo(matrix[start][k].add(matrix[k][j])) > 0
								&& matrix[start][k] != MathNumber.PLUS_INFINITY) {
							matrix[start][j] = matrix[start][k].add(matrix[k][j]);
							path[start][j] = new MathNumber(k);
							// Update all neighbors of start by inductive
							// hypothesis since a value of
							// start has been modified
							Floyd2(matrix, start, n - 1, mem, path);
						} else if (path[start][j] == MathNumber.MINUS_ONE) {
							path[start][j] = new MathNumber(j);
						}

						mem[start][j] = matrix[start][j];

					} else if (matrix[k][j] != MathNumber.PLUS_INFINITY) {
						if (matrix[start][j].compareTo(matrix[start][k].add(mem[k][j])) > 0
								&& matrix[start][k] != MathNumber.PLUS_INFINITY) {
							matrix[start][j] = matrix[start][k].add(mem[k][j]);
							path[start][j] = new MathNumber(k);
							// Update all neighbors of start by inductive
							// hypothesis since a value of
							// start has been modified
							Floyd2(matrix, start, n - 1, mem, path);
						} else if (path[start][j] == MathNumber.MINUS_ONE
								&& matrix[start][j] != MathNumber.PLUS_INFINITY) {
							path[start][j] = new MathNumber(j);
						}

					}
				}
			}
		}
	}

	/**
	 * Detects whether the given graph (represented as a distance matrix)
	 * contains a negative cycle. A negative cycle indicates that the octagon
	 * constraint system is inconsistent and represents the bottom element.
	 * <p>
	 * The algorithm performs a breadth-first search from each vertex, tracking
	 * distances and detecting when a path to a vertex becomes shorter after
	 * visiting it multiple times, which indicates a negative cycle.
	 * </p>
	 * 
	 * @param matrix the distance matrix to check for negative cycles
	 * 
	 * @return {@code true} if a negative cycle is found, {@code false}
	 *             otherwise
	 * 
	 * @throws MathNumberConversionException if number conversion fails during
	 *                                           cycle detection
	 */
	public static boolean HasNegativeCycle(
			MathNumber[][] matrix){

			for(int i=0;i<matrix.length;i++)
			{
				if(matrix[i][i] != MathNumber.ZERO && matrix[i][i] != MathNumber.PLUS_INFINITY)
				{
					return true;
				}
			}

		return false;
	}

	public static int DFS(
			MathNumber[][] matrix,
			int curr,
			String[] colors,
			int dist,
			MathNumber[] pass)
			throws MathNumberConversionException {
		colors[curr] = "Gray";

		for (int i = 0; i < matrix.length; i++) {
			if (colors[i].equals("White") && matrix[curr][i] != MathNumber.PLUS_INFINITY) {
				dist = DFS(matrix, i, colors, dist + matrix[curr][i].toInt(), pass);

				if (dist < 0) {
					return dist;
				}
			} else if (colors[i].equals("Gray") && dist > matrix[curr][i].toInt() + dist
					&& pass[i].compareTo(new MathNumber(matrix.length)) < 0
					&& matrix[curr][i] != MathNumber.PLUS_INFINITY) {
				pass[i] = pass[i].add(new MathNumber(1));
				dist = DFS(matrix, i, colors, dist + matrix[curr][i].toInt(), pass);
			} else if (colors[i].equals("Gray") && dist > matrix[curr][i].toInt() + dist
					&& pass[i].compareTo(new MathNumber(matrix.length)) == 0
					&& matrix[curr][i] != MathNumber.PLUS_INFINITY) {
				pass[i] = pass[i].add(new MathNumber(1));
				return dist;
			}
		}

		// colors[curr] = "Black";

		return 0;

	}

	public static MathNumber[] BFS(
			MathNumber[][] matrix,
			int start,
			String[] colors,
			MathNumber[] dist,
			MathNumber[] pass)
			throws MathNumberConversionException {
		Queue<Integer> q = new LinkedList<>();
		int curr = 0;
		MathNumber path[][] = new MathNumber[matrix.length][matrix.length];

		q.add(start);

		while (!q.isEmpty()) {
			curr = q.remove();

			for (int i = 0; i < matrix.length; i++) {
				if (colors[i].equals("White") && matrix[curr][i] != MathNumber.PLUS_INFINITY) {
					colors[i] = "Gray";
					q.add(i);
					dist[i] = dist[i].add(matrix[curr][i]);
					path[curr][i] = new MathNumber(i);
				} else if (colors[i].equals("Gray") && pass[i].compareTo(new MathNumber(matrix.length)) < 0
						&& dist[i].compareTo(dist[i].add(matrix[curr][i])) > 0
						&& matrix[curr][i] != MathNumber.PLUS_INFINITY) {
					q.add(i);
					dist[i] = dist[i].add(matrix[curr][i]);
					pass[i] = pass[i].add(MathNumber.ONE);
					path[curr][i] = new MathNumber(i);
				} else if (colors[i].equals("Gray") && pass[i].compareTo(new MathNumber(matrix.length)) == 0
						&& dist[i].compareTo(dist[i].add(matrix[curr][i])) > 0
						&& matrix[curr][i] != MathNumber.PLUS_INFINITY) {
					MathNumber[] result = new MathNumber[matrix.length];
					int cur = start;

					dist[i] = dist[i].add(matrix[curr][i]);

					for (int k = 0; k < path[curr].length; k++) {
						result[k] = path[cur][i];
						cur = result[k].toInt();
					}

					return result;
				}
			}

		}

		return null;

	}

	/**
	 * Computes the strong closure of an octagon constraint matrix. This
	 * specialized variant of Floyd-Warshall is optimized for the specific
	 * structure of octagon constraints, where variables appear in pairs (x and
	 * -x) and coherence constraints must be maintained.
	 * <p>
	 * The algorithm iterates through variable pairs and considers all possible
	 * paths through them, including:
	 * <ul>
	 * <li>Direct paths through positive or negative occurrences</li>
	 * <li>Paths that go through both positive and negative occurrences</li>
	 * <li>Coherence constraints ensuring consistency between x and -x</li>
	 * </ul>
	 * After each iteration, the algorithm enforces coherence by ensuring that
	 * constraints involving opposite signs are properly related through
	 * division by 2, as required by the octagon normal form.
	 * </p>
	 * <p>
	 * Time complexity: O(n³) where n is the number of variables (matrix size is
	 * 2n × 2n).
	 * </p>
	 * 
	 * @param matrix the octagon constraint matrix to close (modified in place)
	 */
	public static void strongClosureFloyd(
			MathNumber[][] matrix) {
		int V = matrix.length;

		MathNumber copy[][] = new MathNumber[matrix.length][matrix.length];
		copyArray(copy, matrix);

		for (int k = 1; k <= V / 2; k++) {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < V; j++) {
					MathNumber part1 = MathNumber.ZERO, part2 = MathNumber.ZERO,
							part3 = MathNumber.ZERO, part4 = MathNumber.ZERO;

					// See which cycle is better

					part1 = matrix[i][2 * k - 2].add(matrix[2 * k - 2][j]);
					part3 = matrix[i][2 * k - 2].add(matrix[2 * k - 2][2 * k - 1].add(matrix[2 * k - 1][j]));
					part4 = matrix[i][2 * k - 1].add(matrix[2 * k - 1][2 * k - 2].add(matrix[2 * k - 2][j]));

					part2 = matrix[i][2 * k - 1].add(matrix[2 * k - 1][j]);

					matrix[i][j] = matrix[i][j].min(part1.min(part2.min(part3.min(part4))));
				}
			}
			

			for (int i = 0; i < V; i++) {
				for (int j = 0; j < V; j++) {
					if (i % 2 == 0 && j % 2 == 0) {
						matrix[i][j] = matrix[i][j]
								.min((matrix[i][i + 1].add(matrix[j + 1][j])).divide(new MathNumber(2)));
					} else if (i % 2 == 1 && j % 2 == 0) {
						matrix[i][j] = matrix[i][j]
								.min((matrix[i][i - 1].add(matrix[j + 1][j])).divide(new MathNumber(2)));
					} else if (i % 2 == 0 && j % 2 == 1) {
						matrix[i][j] = matrix[i][j]
								.min((matrix[i][i + 1].add(matrix[j - 1][j])).divide(new MathNumber(2)));
					} else if (i % 2 == 1 && j % 2 == 1) {
						matrix[i][j] = matrix[i][j]
								.min((matrix[i][i - 1].add(matrix[j - 1][j])).divide(new MathNumber(2)));
					}
				}
			}
		}
	}

	/**
	 * Reconstructs and prints the shortest path from a start vertex to a finish
	 * vertex using the path matrix computed by Floyd-Warshall algorithm. Prints
	 * the sequence of vertices along the path, or indicates if no path exists.
	 * 
	 * @param start  the starting vertex
	 * @param finish the destination vertex
	 * @param path   the path matrix containing intermediate vertices
	 * 
	 * @throws MathNumberConversionException if number conversion fails during
	 *                                           path reconstruction
	 */
	public static void printPath(
			int start,
			int finish,
			MathNumber[][] path)
			throws MathNumberConversionException {
		int curr = start;

		System.out.println("Printing path");

		printMatrix(path);

		while (curr != -1 && curr != finish) {
			System.out.println(curr + " ");

			curr = path[curr][finish].toInt();
		}

		if (curr == -1) {
			System.out.println("Path not found");
		} else {
			System.out.println(curr + " ");
		}

		System.out.println("End of path");
	}
}