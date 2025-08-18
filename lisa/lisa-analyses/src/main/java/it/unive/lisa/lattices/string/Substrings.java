package it.unive.lisa.lattices.string;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionInverseSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A lattice structure for substring relations. The domain is implemented as a
 * {@link FunctionalLattice}, mapping identifiers to string expressions,
 * tracking which string expressions are <i>definitely</i> substring of an
 * identifier.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it>">Vincenzo Arceri</a>
 */
public class Substrings extends FunctionalLattice<Substrings, Identifier, ExpressionInverseSet>
		implements
		ValueLattice<Substrings> {

	private static final Substrings TOP = new Substrings(new ExpressionInverseSet().top());

	private static final Substrings BOTTOM = new Substrings(new ExpressionInverseSet().bottom());

	/**
	 * Builds the top abstract value.
	 */
	public Substrings() {
		this(new ExpressionInverseSet());
	}

	/**
	 * Builds the abstract value by cloning the given function.
	 * 
	 * @param lattice  the underlying lattice
	 * @param function the function to clone
	 */
	public Substrings(
			ExpressionInverseSet lattice,
			Map<Identifier, ExpressionInverseSet> function) {
		super(lattice, function);
	}

	private Substrings(
			ExpressionInverseSet lattice) {
		super(lattice);
	}

	@Override
	public Substrings lubAux(
			Substrings other)
			throws SemanticException {
		return functionalLift(
			other,
			lattice.top(),
			this::glbKeys,
			(
					o1,
					o2
			) -> o1.lub(o2)).clear();
	}

	@Override
	public Substrings glbAux(
			Substrings other)
			throws SemanticException {
		return functionalLift(
			other,
			lattice.top(),
			this::lubKeys,
			(
					o1,
					o2
			) -> o1.glb(o2)).closure();
	}

	@Override
	public Substrings top() {
		return isTop() ? this : TOP;
	}

	@Override
	public Substrings bottom() {
		return isBottom() ? this : BOTTOM;
	}

	@Override
	public ExpressionInverseSet stateOfUnknown(
			Identifier key) {
		return lattice.top();
	}

	@Override
	public Substrings mk(
			ExpressionInverseSet lattice,
			Map<Identifier, ExpressionInverseSet> function) {
		return new Substrings(lattice.isBottom() ? lattice.bottom() : lattice.top(), function);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		if (id == null || function == null)
			return false;

		if (function.containsKey(id))
			return true;

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			for (SymbolicExpression expr : entry.getValue().elements) {
				if (appears(id, expr))
					return true;
			}
		}

		return false;
	}

	@Override
	public Substrings forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (!knowsIdentifier(id))
			return this;

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);
		newFunction.remove(id);
		newFunction.replaceAll(
			(
					key,
					value
			) -> removeFromSet(value, id));

		return mk(lattice, newFunction);
	}

	@Override
	public Substrings forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (function == null || function.keySet().isEmpty())
			return this;

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);
		ids.forEach(id -> {
			newFunction.remove(id);
			newFunction.replaceAll(
				(
						key,
						value
				) -> removeFromSet(value, id));
		});

		return mk(lattice, newFunction);
	}

	@Override
	public Substrings forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (function == null || function.keySet().isEmpty())
			return this;

		// Get all identifiers
		Set<Identifier> ids = new HashSet<>();
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			ids.add(entry.getKey());
			for (SymbolicExpression s : entry.getValue().elements()) {
				if (s instanceof Identifier) {
					Identifier id = (Identifier) s;
					ids.add(id);
				}
			}
		}

		Substrings result = mk(lattice, mkNewFunction(function, false));

		for (Identifier id : ids) {
			// Test each identifier
			if (test.test(id)) {
				result = result.forgetIdentifier(id, pp);
			}
		}

		return result;
	}

	@Override
	public Substrings pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Substrings(lattice.pushScope(token, pp), mkNewFunction(function, true));
	}

	@Override
	public Substrings popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Substrings(lattice.popScope(token, pp), mkNewFunction(function, true));
	}

	/**
	 * Adds a set of expressions ({@code exprs}) for {@code id} to the this
	 * abstract value.
	 * 
	 * @param exprs the set of symbolic expressions
	 * @param id    the identifier
	 * 
	 * @return a new abstract value with the added expressions
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings add(
			Set<SymbolicExpression> exprs,
			Identifier id)
			throws SemanticException {

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		// Don't add the expressions that contain the key variable (ex: x ->
		// x,
		// x -> x + y will not be added)
		Set<SymbolicExpression> expressionsToAdd = new HashSet<>();
		for (SymbolicExpression se : exprs) {
			if (!appears(id, se))
				expressionsToAdd.add(se);
		}

		if (expressionsToAdd.isEmpty())
			return this;

		ExpressionInverseSet newSet = new ExpressionInverseSet(expressionsToAdd);

		if (!(newFunction.get(id) == null))
			newSet = newSet.glb(newFunction.get(id));

		newFunction.put(id, newSet);

		return mk(lattice, newFunction);
	}

	/**
	 * Adds a single expression ({@code expr}) for {@code id} to the this
	 * abstract value.
	 * 
	 * @param expr the symbolic expression
	 * @param id   the identifier
	 * 
	 * @return a new abstract value with the added expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings add(
			SymbolicExpression expr,
			Identifier id)
			throws SemanticException {
		Set<SymbolicExpression> expression = new HashSet<>();
		expression.add(expr);

		return add(expression, id);
	}

	/**
	 * First step of assignment, removing obsolete relations.
	 * 
	 * @param extracted Expression assigned
	 * @param id        Expression getting assigned
	 * 
	 * @return Copy of the domain, with the holding relations after the
	 *             assignment.
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings remove(
			Set<SymbolicExpression> extracted,
			Identifier id) {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		// If assignment is similar to x = x + ..., then we keep current
		// relations to x, otherwise we remove them.
		if (!extracted.contains(id)) {
			newFunction.remove(id);
		}

		// Remove relations containing id from the other entries
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : newFunction.entrySet()) {
			ExpressionInverseSet newSet = removeFromSet(entry.getValue(), id);

			entry.setValue(newSet);
		}

		return mk(lattice, newFunction);
	}

	/*
	 * Returns a set without expressions containing id starting from source
	 */
	private static ExpressionInverseSet removeFromSet(
			ExpressionInverseSet source,
			Identifier id) {
		Set<SymbolicExpression> newSet = source.elements.stream()
			.filter(element -> !appears(id, element))
			.collect(Collectors.toSet());

		return newSet.isEmpty() ? new ExpressionInverseSet().top() : new ExpressionInverseSet(newSet);
	}

	/**
	 * Performs the inter-assignment phase.
	 * 
	 * @param assignedId         Variable getting assigned
	 * @param assignedExpression Expression assigned
	 * 
	 * @return Copy of the domain with new relations following the
	 *             inter-assignment phase
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings interasg(
			Identifier assignedId,
			SymbolicExpression assignedExpression)
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		if (!knowsIdentifier(assignedId))
			return this;

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			// skip same entry
			if (entry.getKey().equals(assignedId))
				continue;

			if (entry.getValue().contains(assignedExpression)) {
				Set<SymbolicExpression> newRelation = new HashSet<>();
				newRelation.add(assignedId);

				ExpressionInverseSet newSet = newFunction.get(entry.getKey())
					.glb(new ExpressionInverseSet(newRelation));
				newFunction.put(entry.getKey(), newSet);
			}

		}

		return mk(lattice, newFunction);
	}

	/**
	 * Performs the closure over an identifier. The method adds to {@code id}
	 * the expressions found in the variables mapped to {@code id}
	 * 
	 * @param id the identifier to perform the closure on
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings closure(
			Identifier id)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Substrings result = mk(lattice, mkNewFunction(function, false));

		ExpressionInverseSet set = result.getState(id);

		for (SymbolicExpression se : set) {
			if (se instanceof Variable) {
				Variable variable = (Variable) se;

				// Variable found --> add the relations of variable to id

				if (result.knowsIdentifier(variable)) {
					Set<SymbolicExpression> add = new HashSet<>(result.getState(variable).elements);
					result = result.add(add, id);
				}
			}
		}

		return result;
	}

	/**
	 * Performs the closure over the domain.
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings closure()
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Substrings prev;
		Substrings result = mk(lattice, mkNewFunction(function, false));

		do {
			prev = mk(lattice, mkNewFunction(result.function, false));
			Set<Identifier> set = prev.function.keySet();

			for (Identifier id : set) {
				// Perform the closure on every identifier of the domain
				result = result.closure(id);
			}

			result = result.clear();

			// Some relations may be added; check that close is applied
			// correctly to the added relations
		} while (!prev.equals(result));

		return result;

	}

	/**
	 * Clears the domain, removing all relations that are top.
	 * 
	 * @return a copy of the domain with the top relations removed
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Substrings clear()
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newMap = mkNewFunction(function, false);

		newMap.entrySet().removeIf(entry -> entry.getValue().isTop());

		return new Substrings(lattice, newMap);
	}

	private static boolean appears(
			Identifier id,
			SymbolicExpression expr) {
		if (expr instanceof Identifier)
			return id.equals(expr);

		if (expr instanceof Constant)
			return false;

		if (expr instanceof BinaryExpression) {
			BinaryExpression expression = (BinaryExpression) expr;
			SymbolicExpression left = expression.getLeft();
			SymbolicExpression right = expression.getRight();

			return appears(id, left) || appears(id, right);
		}

		return false;
	}

	@Override
	public Substrings store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (isTop() || isBottom() || function == null || !function.containsKey(source))
			return this;
		return putState(target, getState(source));
	}

}