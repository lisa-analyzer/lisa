package it.unive.lisa.analysis.string;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionInverseSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;

public class SubstringDomain extends FunctionalLattice<SubstringDomain, Identifier, ExpressionInverseSet> implements ValueDomain<SubstringDomain> {

	public SubstringDomain(ExpressionInverseSet lattice, Map<Identifier, ExpressionInverseSet> function) {
		super(lattice, function);
	}
	
	public SubstringDomain(ExpressionInverseSet lattice) {
		super(lattice);
	}
	
	public SubstringDomain() {
		this(new ExpressionInverseSet());
	}
	
	@Override
	public SubstringDomain lub(SubstringDomain other) throws SemanticException{
		if (other == null)
			throw new SemanticException("other == null");
		
		if (this == other || this.equals(other))
			return this;

		if (other.isTop() || this.isTop())
			return top();
		
		if (isBottom() || other.isBottom())
			return bottom();
		
		 return lubAux(other);

	}
	
	@Override
	public SubstringDomain lubAux(SubstringDomain other) throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(null, false);
		
		for(Map.Entry<Identifier, ExpressionInverseSet> entry : other) {
			ExpressionInverseSet newSet = getState(entry.getKey());
			newFunction.put(entry.getKey(), newSet.lub(entry.getValue()));
		}
		
		SubstringDomain result = mk(lattice, newFunction);
		
		return result.clear();
	}
	
	@Override
	public SubstringDomain glb(SubstringDomain other) throws SemanticException{
		if (other == null)
			throw new SemanticException("other == null");
		
		if (this == other || this.equals(other))
			return this;
		
		if (isTop() || this.isBottom())
			return other;
		
		if (other.isTop() || other.isBottom())
			return this;

		return glbAux(other);
	}
	
	@Override
	public SubstringDomain glbAux(SubstringDomain other) throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(this.function, false);
		
		for(Map.Entry<Identifier, ExpressionInverseSet> entry : other) {
			ExpressionInverseSet newSet = getState(entry.getKey()).glb(entry.getValue());
			newFunction.put(entry.getKey(), newSet);
		}
		
		SubstringDomain result = mk(lattice, newFunction);
		
		return result.closure();
		
	}

	@Override
	public SubstringDomain top() {
		return isTop() ? this : new SubstringDomain(lattice.top());
	}

	@Override
	public SubstringDomain bottom() {
		return isBottom() ? this : new SubstringDomain(lattice.bottom());
	}

	@Override
	public ExpressionInverseSet stateOfUnknown(Identifier key) {
		return new ExpressionInverseSet();
	}

	@Override
	public SubstringDomain mk(ExpressionInverseSet lattice, Map<Identifier, ExpressionInverseSet> function) {
		return new SubstringDomain(lattice, function);
	}

	@Override
	public SubstringDomain assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> identifiers = extrPlus(expression);
		
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		result = result.remove(identifiers, id);
		
		result = result.add(identifiers, id);
		
		result = result.interasg(id);
		
		result = result.closure(id);
				
		return result.clear();
	}

	@Override
	public SubstringDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public SubstringDomain assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {
		
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		if (expression instanceof BinaryExpression) {
			
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			
			SymbolicExpression left = binaryExpression.getLeft();
			SymbolicExpression right = binaryExpression.getRight();
			
			if (binaryOperator instanceof StringContains 
					|| binaryOperator instanceof StringStartsWith
					|| binaryOperator instanceof StringEndsWith) {
				
				if (!(left instanceof Identifier))
					return this;
				
				if (!(right instanceof ValueExpression))
					throw new SemanticException("instanceof right");
				
				Set<SymbolicExpression> extracted = extrPlus((ValueExpression) right);
				
				result = result.add(extracted, (Identifier) left);
				
				//result = result.closure((Identifier) left);
				result = result.closure();
				
			} else if (binaryOperator instanceof StringEquals){
				//both are identifiers
				if ((left instanceof Identifier) && (right instanceof Identifier)) {
					result = result.add(extrPlus((ValueExpression) left), (Identifier) right);
					result = result.add(extrPlus((ValueExpression) right), (Identifier)left);
					
					result = result.closure((Identifier) left);
					result = result.closure((Identifier) right);
				} //one is identifier
				else if((left instanceof Identifier) || (right instanceof Identifier)) {
					
					if (right instanceof Identifier) { //left instance of Identifier, right SymbolicExpression
						SymbolicExpression temp = left;
				        left = right;
				        right = temp;
					}
					
					if (!(right instanceof ValueExpression))
						throw new SemanticException("instanceof right != ValueExpression.class");
					
					Set<SymbolicExpression> add = extrPlus((ValueExpression) right);
					
					result = result.add(add, (Identifier) left);
					
					//result = result.closure((Identifier) left);
					result = result.closure();

				}			
			} else if(binaryOperator instanceof LogicalOr || binaryOperator instanceof LogicalAnd){
				
				if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
					throw new SemanticException("!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
				
				ValueExpression rightValueExpression = (ValueExpression) right;
				ValueExpression leftValueExpression = (ValueExpression) left;
				
				SubstringDomain leftDomain = assume(leftValueExpression, src, dest, oracle);
				SubstringDomain rightDomain =  assume(rightValueExpression, src, dest, oracle);
				
				if (binaryOperator instanceof LogicalOr) {
					result = leftDomain.glb(rightDomain);
				} else {
					result = leftDomain.lub(rightDomain);
				}
			}
				
		}
		
		return result.clear();
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		if (id == null || function == null || isBottom() || isTop())
			return false;
		
		if (function.containsKey(id))
			return true;
		
		return false;		
	}

	@Override
	public SubstringDomain forgetIdentifier(Identifier id) throws SemanticException {
		if (!knowsIdentifier(id))
			return this;
		
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false); //function != null
		
		newFunction.remove(id);
		
		return new SubstringDomain(lattice, newFunction);
	}

	@Override
	public SubstringDomain forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (function == null || isTop() || isBottom() || function.keySet().isEmpty())
			return this;
		
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false); //function != null
		
		Set<Identifier> keys = newFunction.keySet().stream().filter(test::test).collect(Collectors.toSet());
		
		keys.forEach(newFunction::remove);
		
		return new SubstringDomain(lattice, newFunction);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (isBottom() || !(expression instanceof BinaryExpression))
			return Satisfiability.UNKNOWN;
		
		
		BinaryExpression binaryExpression = (BinaryExpression) expression;
		BinaryOperator binaryOperator = binaryExpression.getOperator();
		
		SymbolicExpression left = binaryExpression.getLeft();
		SymbolicExpression right = binaryExpression.getRight();
		
		if (binaryOperator instanceof StringContains) {
			if (!(left instanceof Variable))
				return Satisfiability.UNKNOWN;
			
			return getState((Identifier) left).contains(right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
		} else if (binaryOperator instanceof StringEquals || binaryOperator instanceof StringEndsWith || binaryOperator instanceof StringStartsWith) {
			if (!(left instanceof Variable) || !(right instanceof Variable))
				return Satisfiability.UNKNOWN;
			
			return (getState((Identifier) left).contains(right)) && (getState((Identifier) right).contains(left)) 
					? Satisfiability.SATISFIED 
					: Satisfiability.UNKNOWN;
		} else if (binaryOperator instanceof LogicalOr) {
			if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
				throw new SemanticException("!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
			Satisfiability leftSatisfiability = satisfies((ValueExpression) left, pp, oracle);
			
			if (leftSatisfiability == Satisfiability.SATISFIED)
				return Satisfiability.SATISFIED;
			
			Satisfiability rightSatisfiability = satisfies((ValueExpression) right, pp, oracle);
			
			return rightSatisfiability;
			
		} else if (binaryOperator instanceof LogicalAnd) {
			if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
				throw new SemanticException("!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
			Satisfiability leftSatisfiability = satisfies((ValueExpression) left, pp, oracle);
			Satisfiability rightSatisfiability = satisfies((ValueExpression) right, pp, oracle);

			if (leftSatisfiability == Satisfiability.SATISFIED && rightSatisfiability == Satisfiability.SATISFIED)
				return Satisfiability.SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		}
		
		throw new SemanticException("Invalid expression");
	}

	@Override
	public SubstringDomain pushScope(ScopeToken token) throws SemanticException {
		return new SubstringDomain(lattice.pushScope(token), mkNewFunction(function, true));
	}

	@Override
	public SubstringDomain popScope(ScopeToken token) throws SemanticException {
		return new SubstringDomain(lattice.popScope(token), mkNewFunction(function, true));
	}
	
	private static Set<SymbolicExpression> extr(ValueExpression expression) throws SemanticException{
		
		Set<SymbolicExpression> result = new HashSet<>();
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			if (!(binaryOperator instanceof StringConcat))
				throw new SemanticException();
			
			ValueExpression left = (ValueExpression) binaryExpression.getLeft();
			ValueExpression right = (ValueExpression) binaryExpression.getRight();
			
			result.addAll(extr(left));
			result.addAll(extr(right));
		} else if(expression instanceof Variable || expression instanceof Constant) {			
			result.add(expression);
		} 
		
		return result;
	}
	
	private static Set<SymbolicExpression> extrPlus(ValueExpression expression) throws SemanticException{
		Set<SymbolicExpression> result = extr(expression);
		
		return result;
	}
	
	
	private SubstringDomain add(Set<SymbolicExpression> symbolicExpressions, Identifier id) throws SemanticException{
		
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);
		
		symbolicExpressions.remove(id);
		
		ExpressionInverseSet newSet = new ExpressionInverseSet(symbolicExpressions);
		
		if (!(newFunction.get(id) == null))
			newSet = newSet.glb(newFunction.get(id));
		
		newFunction.put(id, newSet);
		
		return mk(lattice, newFunction);
	}
	
	private SubstringDomain remove(Set<SymbolicExpression> extracted, Identifier id) throws SemanticException {	
		
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);
		
		if(!extracted.contains(id)) { // x = x + ..... --> keep relations for x
			newFunction.remove(id);
		}
		
		Set<SymbolicExpression> expressionsToRemove = new HashSet<>();
		for (SymbolicExpression expression : extracted) {
			if (appears(id, expression))
				expressionsToRemove.add(expression);
		}
		
		expressionsToRemove.add(id);
		
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : newFunction.entrySet()) {
			for(SymbolicExpression se : expressionsToRemove) {
				Set<SymbolicExpression> set = entry.getValue().elements();
				
				set.remove(se);
			}
		}
		
		return mk(lattice, newFunction);
	}
	
	private SubstringDomain interasg(Identifier id) throws SemanticException {	
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);
		
		ExpressionInverseSet compare = function.get(id);

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			if (entry.getKey().equals(id))
				continue;
			
			if ((entry.getValue().lub(compare)).equals(compare)) {
				Set<SymbolicExpression> newRelation = new HashSet<>();
				newRelation.add(id);
				
				ExpressionInverseSet newSet = newFunction.get(entry.getKey()).glb(new ExpressionInverseSet(newRelation));
				newFunction.put(entry.getKey(), newSet);
			}
				
		}
		
		return mk(lattice, newFunction);
	}
	
	private SubstringDomain closure(Identifier id) throws SemanticException {
		
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		ExpressionInverseSet toModify;
		ExpressionInverseSet iterate;
		
		do {
			toModify = result.getState(id);
			iterate = toModify.mk(toModify.elements);
		
			for (SymbolicExpression se : toModify) {
				if (se instanceof Variable) {
					Variable variable = (Variable) se;
					
					if (result.knowsIdentifier(variable)) {
						Set<SymbolicExpression> add = new HashSet<>(result.getState(variable).elements);
						result = result.add(add , id);
					}
				}
			}
		} while(!iterate.equals(toModify));	
		
		return result;
	}
	
	private SubstringDomain closure() throws SemanticException {
		
		SubstringDomain prev;
		SubstringDomain result =  mk(lattice, mkNewFunction(function, false));

		
		do {
			prev =  mk(lattice, mkNewFunction(result.function, false));
			Set<Identifier> set = prev.function.keySet();
			
			for(Identifier id : set) {
				result = result.closure(id);
			}
			
			result = result.clear();
		}while(!prev.equals(result));
		
		
		return result;
			
	}
	
	private SubstringDomain clear() throws SemanticException {
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		Map<Identifier, ExpressionInverseSet> iterate = mkNewFunction(result.function, false);
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : iterate.entrySet() ) {
			if (entry.getValue().isBottom()) {
				result = result.forgetIdentifier(entry.getKey());
			}
				
		}
		
		return result;
	}
	
	private static boolean appears(Identifier id, SymbolicExpression expr) throws SemanticException {	
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
		
		throw new SemanticException("Invalid expression");
		
		

	}

}
