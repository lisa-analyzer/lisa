package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.string.StringConstantPropagation.SCP;
import it.unive.lisa.analysis.string.SubstringDomain.Subs;
import it.unive.lisa.analysis.value.ValueDomain;
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
 * enriched with string constant propagation. This domain tracks the Cartesian
 * product between {@link SubstringDomain.Subs} and
 * {@link StringConstantPropagation.SCP}. This domain follows the one defined
 * <a href="https://link.springer.com/chapter/10.1007/978-3-030-94583-1_2">in
 * this paper</a>.
 * 
 * @author <a href="mailto:michele.martelli1@studenti.unipr.it">Michele
 *             Martelli</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class SubstringDomainWithConstants
		implements
		ValueDomain<ValueLatticeProduct<ValueEnvironment<SCP>, Subs>> {

	private final StringConstantPropagation scp = new StringConstantPropagation();

	private final SubstringDomain subs = new SubstringDomain();

	@Override
	public ValueLatticeProduct<ValueEnvironment<SCP>, Subs> makeLattice() {
		return new ValueLatticeProduct<>(scp.makeLattice(), subs.makeLattice());
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<SCP>, Subs> assign(
			ValueLatticeProduct<ValueEnvironment<SCP>, Subs> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		// expresson must be a string expression
		if (oracle != null
				&& pp != null
				&& oracle.getRuntimeTypesOf(expression, pp).stream().allMatch(t -> !t.isStringType() && !t.isUntyped()))
			return state;

		ValueEnvironment<SCP> a = scp.assign(state.first, id, expression, pp, oracle);
		Subs b = subs.assign(state.second, id, expression, pp, oracle);

		SCP constantValue = a.getState(id);

		if (!constantValue.isTop() && !constantValue.isBottom()) {
			for (Entry<Identifier, SCP> elem : a) {
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
			ValueExpression newExpression = new BinaryExpression(
					boolType,
					id,
					constant,
					StringEquals.INSTANCE,
					SyntheticLocation.INSTANCE);
			b = subs.assume(b, newExpression, pp, pp, oracle);
		}

		return new ValueLatticeProduct<>(a, b);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<SCP>, Subs> smallStepSemantics(
			ValueLatticeProduct<ValueEnvironment<SCP>, Subs> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<SCP>, Subs> assume(
			ValueLatticeProduct<ValueEnvironment<SCP>, Subs> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

}
