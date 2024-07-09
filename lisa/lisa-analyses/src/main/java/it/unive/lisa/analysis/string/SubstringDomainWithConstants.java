package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueCartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.type.Type;
import java.util.Map.Entry;

/**
 * The substring relational abstract domain (see {@link SubstringDomain})
 * enriched with string constant propagation. This domain is defined as the
 * Cartesian product between {@link SubstringDomain} and
 * {@link StringConstantPropagation}. This domain follows the one defined
 * <a href="https://link.springer.com/chapter/10.1007/978-3-030-94583-1_2">in
 * this paper</a>.
 * 
 * @author <a href="mailto:michele.martelli1@studenti.unipr.it">Michele
 *             Martelli</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class SubstringDomainWithConstants
		extends
		ValueCartesianProduct<ValueEnvironment<StringConstantPropagation>, SubstringDomain> {

	/**
	 * Builds the abstract value starting from components.
	 * 
	 * @param left  the string constant propagation environment
	 * @param right the substring domain abstract value
	 */
	public SubstringDomainWithConstants(
			ValueEnvironment<StringConstantPropagation> left,
			SubstringDomain right) {
		super(left, right);
	}

	/**
	 * Builds the top abstract value.
	 */
	public SubstringDomainWithConstants() {
		this(new ValueEnvironment<StringConstantPropagation>(new StringConstantPropagation()), new SubstringDomain());
	}

	@Override
	public SubstringDomainWithConstants assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		// expresson must be a string expression
		if (oracle != null && pp != null && oracle.getRuntimeTypesOf(expression, pp, oracle).stream()
				.allMatch(t -> !t.isStringType() && !t.isUntyped()))
			return this;

		ValueEnvironment<StringConstantPropagation> a = this.left.assign(id, expression, pp, oracle);
		SubstringDomain b = this.right.assign(id, expression, pp, oracle);

		StringConstantPropagation constantValue = a.getState(id);

		if (!constantValue.isTop() && !constantValue.isBottom()) {
			for (Entry<Identifier, StringConstantPropagation> elem : a) {
				if (elem.getKey().equals(id))
					continue;

				if (elem.getValue().equals(constantValue)) {
					b = b.add(elem.getKey(), id).add(id, elem.getKey()).closure();
				}
			}

			Type strType = pp.getProgram().getTypes().getStringType();
			Type boolType = pp.getProgram().getTypes().getBooleanType();

			String stringConstantValue = constantValue.getValue();
			Constant constant = new Constant(strType, stringConstantValue, SyntheticLocation.INSTANCE);
			ValueExpression newExpression = new BinaryExpression(boolType, id, constant, StringEquals.INSTANCE,
					SyntheticLocation.INSTANCE);
			b = b.assume(newExpression, pp, pp, oracle);
		}

		return mk(a, b);
	}

	@Override
	public SubstringDomainWithConstants mk(
			ValueEnvironment<StringConstantPropagation> left,
			SubstringDomain right) {
		return new SubstringDomainWithConstants(left, right);
	}
}