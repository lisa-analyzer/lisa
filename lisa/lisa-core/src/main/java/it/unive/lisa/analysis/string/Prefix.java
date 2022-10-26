package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

import java.util.Objects;

public class Prefix extends BaseNonRelationalValueDomain<Prefix>{

    private final String prefix;

    private final static Prefix TOP = new Prefix();
    private final static Prefix BOTTOM = new Prefix(null);

    public Prefix(){
        this("");
    }

    public Prefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    protected Prefix lubAux(Prefix other) throws SemanticException {
        String otherPrefixString = other.getPrefix();
        StringBuilder result = new StringBuilder();

        int i = 0;
        while(prefix.charAt(i) == otherPrefixString.charAt(i)){
            result.append(prefix.charAt(i++));
        }

        if(result.length() != 0)
            return new Prefix(result.toString());
        else
            return TOP;
    }

    @Override
    protected Prefix wideningAux(Prefix other) throws SemanticException {
        return lubAux(other);
    }

    @Override
    protected boolean lessOrEqualAux(Prefix other) throws SemanticException {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prefix prefix1 = (Prefix) o;
        return Objects.equals(prefix, prefix1.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }

    @Override
    public Prefix top() {
        return TOP;
    }

    @Override
    public Prefix bottom() {
        return BOTTOM;
    }

    @Override
    public DomainRepresentation representation() {
        return null;
    }

    @Override
    protected Prefix evalNullConstant(ProgramPoint pp){
        return TOP;
    }

    @Override
    protected Prefix evalNonNullConstant(Constant constant, ProgramPoint pp){
        if(constant.getValue() instanceof String)
            return new Prefix( (String) constant.getValue() );

        return TOP;
    }

    @Override
    protected Prefix evalUnaryExpression(UnaryOperator operator, Prefix arg, ProgramPoint pp){
        if(operator == StringLength.INSTANCE){
            return this;
        }
        else
            return TOP;
    }

    @Override
    protected Prefix evalBinaryExpression(BinaryOperator operator, Prefix left, Prefix right, ProgramPoint pp) throws SemanticException {
        if(operator == StringConcat.INSTANCE){
            return left;
        }
        else if(operator == StringContains.INSTANCE){
            return left.lubAux(right);
        }

        else if(operator == StringEndsWith.INSTANCE){
            return left.lubAux(right);
        }

        else if(operator == StringEquals.INSTANCE){
            return left.lubAux(right);
        }

        else if(operator == StringIndexOf.INSTANCE){
            return left.lubAux(right);
        }

        else if(operator == StringStartsWith.INSTANCE){
            return left.lubAux(right);
        }

        return TOP;
    }

    private String getPrefix(){
        return this.prefix;
    }
}
