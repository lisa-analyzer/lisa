package it.unive.lisa.analysis.string;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueCartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;


public class SubstringDomainWithConstants extends ValueCartesianProduct<ValueEnvironment<StringConstantPropagation>, SubstringDomain>{
	public SubstringDomainWithConstants(ValueEnvironment<StringConstantPropagation> left, SubstringDomain right) {
		super(left, right);
	}
	
	public SubstringDomainWithConstants() {
		this(new ValueEnvironment<StringConstantPropagation>(new StringConstantPropagation()), new SubstringDomain());
	}
	
	@Override
	public SubstringDomainWithConstants assign(Identifier id,
			ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		
		
		ValueEnvironment<StringConstantPropagation> a = this.left.assign(id, expression, pp, oracle);
		SubstringDomain b = this.right.assign(id, expression, pp, oracle);
		
		StringConstantPropagation compare = a.getState(id);

		if (!compare.isTop() && !compare.isBottom()) {
			for (Entry<Identifier, StringConstantPropagation> elem : a) {
				if (elem.getKey().equals(id))
					continue;
				
				if (elem.getValue().equals(compare)) {
					Set<SymbolicExpression> add = new HashSet<>();
					add.add(elem.getKey());
					add.add(id);
					
					b = b.add(add , id).add(add, elem.getKey()).closure();
				}
			}
		}
			
		return mk(a, b);
	}
	
	@Override
	public SubstringDomainWithConstants mk(
			ValueEnvironment<StringConstantPropagation> left, SubstringDomain right) {
		return new SubstringDomainWithConstants(left, right);
	}
}