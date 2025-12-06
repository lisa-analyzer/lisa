package it.unive.lisa.symbolic.value.operator.nary;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

public interface NaryOperator extends Operator {

    /**
     * Computes the runtime types of this expression (i.e., of the result of
     * this expression) assuming that the arguments of this expression have the
     * given types.
     *
     * @param types  the type system knowing about the types of the current
     *                   program
     * @param operands array containing the set of types of every operands involved in the operation
     *
     * @return the runtime types of this expression
     */
    Set<Type> typeInference(
            TypeSystem types,
            Set<Type>[] operands);
}