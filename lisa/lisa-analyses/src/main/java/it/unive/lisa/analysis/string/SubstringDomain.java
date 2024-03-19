package it.unive.lisa.analysis.string;

import java.util.Collections;
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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;

public class SubstringDomain extends FunctionalLattice<SubstringDomain, Identifier, ExpressionInverseSet> implements ValueDomain<SubstringDomain> {

	public SubstringDomain(ExpressionInverseSet lattice, Map<Identifier, ExpressionInverseSet> function) {
		super(lattice, function);
	}
	
	public SubstringDomain(ExpressionInverseSet lattice) {
		super(lattice);
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
		if (isBottom())
			return this;
		
		Set<SymbolicExpression> identifiers = extrPlus(expression);
		
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		result.remove(identifiers, id);
		
		result.add(identifiers, id);
		
		result.interasg(id);
		
		result.closure(id);
				
		return result;
	}

	@Override
	public SubstringDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public SubstringDomain assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {
		if (isBottom())
			return this;
		
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));
		
		if (expression instanceof BinaryExpression) {
			
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			
			SymbolicExpression left = binaryExpression.getLeft();
			SymbolicExpression right = binaryExpression.getRight();
			
			if (binaryOperator instanceof StringContains) {
				
				if (!(left instanceof Identifier))
					return this;
				
				if (!(right instanceof ValueExpression))
					throw new SemanticException("instanceof right");
								
				
				Set<SymbolicExpression> add = extrPlus((ValueExpression) right);
				
				result.add(add, (Identifier) left);
				
				result.closure((Identifier) left);
				
			} else if (binaryOperator instanceof StringEquals){
				//both are identifiers
				if ((left instanceof Identifier) && (right instanceof Identifier)) {
					result.add(extrPlus((ValueExpression) left), (Identifier) right);
					result.add(extrPlus((ValueExpression) right), (Identifier)left);
					
					result.closure((Identifier) left);
					result.closure((Identifier) right);
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
					
					result.add(add, (Identifier) left);
					
					result.closure((Identifier) left);
				}			
			} else if(binaryOperator instanceof LogicalOr || binaryOperator instanceof LogicalAnd){
				
				if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
					throw new SemanticException("!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
				
				ValueExpression rightValueExpression = (ValueExpression) right;
				ValueExpression leftValueExpression = (ValueExpression) left;
				
				SubstringDomain leftDomain = assume(leftValueExpression, src, dest, oracle);
				SubstringDomain rightDomain =  assume(rightValueExpression, src, dest, oracle);
				
				if (binaryOperator instanceof LogicalOr) {
					result = glb(leftDomain.glb(rightDomain));
				} else {
					result = glb(leftDomain.lub(rightDomain));
				}
			} 
				
		}
		
		return result;
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
		if (isBottom())
			return Satisfiability.BOTTOM;
		
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			
			SymbolicExpression left = binaryExpression.getLeft();
			SymbolicExpression right = binaryExpression.getRight();
			
			if (binaryOperator instanceof StringContains) {
				if (!(left instanceof Identifier))
					return Satisfiability.UNKNOWN;
				
				return function.get((Identifier) left).contains(right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
			} else if (binaryOperator instanceof StringEquals) {
				if (!(left instanceof Identifier) || !(right instanceof Identifier))
					return Satisfiability.UNKNOWN;
				
				return (function.get((Identifier) left).contains(right)) && (function.get((Identifier) right).contains(left)) 
						? Satisfiability.SATISFIED 
						: Satisfiability.UNKNOWN;
			}
		}
		
		return Satisfiability.UNKNOWN;
	}

	@Override
	public SubstringDomain pushScope(ScopeToken token) throws SemanticException {
		return new SubstringDomain(lattice.pushScope(token), mkNewFunction(function, true));
	}

	@Override
	public SubstringDomain popScope(ScopeToken token) throws SemanticException {
		return new SubstringDomain(lattice.popScope(token), mkNewFunction(function, true));
	}
	
	private Set<SymbolicExpression> extr(ValueExpression expression) throws SemanticException{
		if (isBottom())
			return null;
		
		Set<SymbolicExpression> result = Collections.emptySet();
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			if (!(binaryOperator instanceof StringConcat))
				throw new SemanticException();
			
			ValueExpression left = (ValueExpression) binaryExpression.getLeft();
			ValueExpression right = (ValueExpression) binaryExpression.getRight();
			
			result.addAll(extr(left));
			result.addAll(extr(right));
		} else if(expression instanceof Identifier) {
			Identifier identifier = (Identifier) expression;
			
			result.add(identifier);
		} else {
			throw new SemanticException("typeof expression");
		}
		
		return result;
	}
	
	private Set<SymbolicExpression> extrPlus(ValueExpression expression) throws SemanticException{
		if (isBottom())
			return null;
		
		Set<SymbolicExpression> result = extr(expression);
		
		return result;
	}
	
	
	private void add(Set<SymbolicExpression> symbolicExpressions, Identifier id) throws SemanticException{
		if (isBottom())
			return;
		
		ExpressionInverseSet newSet = function.get(id).glb(new ExpressionInverseSet(symbolicExpressions));
		
		function.put(id, newSet);
	}
	
	private void remove(Set<SymbolicExpression> expresionsToRemove, Identifier id) {	
		if(isBottom())
			return;
		
		if(!expresionsToRemove.contains(id)) { // x = x + ..... --> keep relations for x
			function.remove(id);
		}
		
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			for(SymbolicExpression se : expresionsToRemove) {
				Set<SymbolicExpression> set = entry.getValue().elements();
				
				set.remove(se);
			}
		}
	}
	
	private void interasg(Identifier id) {
		if (isBottom())
			return;
		
		Set<Identifier> keys = getKeys();
		keys.remove(id);
		
		ExpressionInverseSet eiv = function.get(id);
		Set<SymbolicExpression> iterate = eiv.mk(function.get(id).elements()).elements();
		
		
		for (Identifier key : keys) {
			boolean contained = true;
			for (SymbolicExpression symbolicExpression : iterate) {
				if (!function.get(key).contains(symbolicExpression)) {
					contained = false;
					break;
				}
			}
			
			if (contained) {
				ExpressionInverseSet set = function.get(key);
				set.elements.add(id);
			}
			
		}
	}
	
	private void closure(Identifier id) {
		if (isBottom())
			return;
		
		ExpressionInverseSet toModify = function.get(id);
		Set<SymbolicExpression> setToModify = toModify.elements();
		Set<SymbolicExpression> iterate = toModify.mk(setToModify).elements();
		
		do {
			for (SymbolicExpression se : iterate) {
				if (se instanceof Identifier) {
					Identifier variable = (Identifier) se;
					
					setToModify.addAll(function.get(variable).elements());
				}
			}
		} while(iterate.equals(setToModify));
			
	}

}
