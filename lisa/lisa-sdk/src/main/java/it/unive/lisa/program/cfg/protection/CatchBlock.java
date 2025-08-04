package it.unive.lisa.program.cfg.protection;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;

public class CatchBlock {

    private final Type[] exceptions;

    private final VariableRef identifier;

    private final Collection<Statement> body;

    private final Statement head;

    public CatchBlock(VariableRef identifier, Statement head, Collection<Statement> body, Type... exceptions) {
        this.exceptions = exceptions;
        this.identifier = identifier;
        this.body = body;
        this.head = head;
    }

    public Type[] getExceptions() {
        return exceptions;
    }

    public VariableRef getIdentifier() {
        return identifier;
    }

    public Collection<Statement> getBody() {
        return body;
    }

    public Statement getHead() {
        return head;
    }

    public void simplify() {
        body.removeIf(NoOp.class::isInstance);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exceptions == null) ? 0 : Arrays.hashCode(exceptions));
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((head == null) ? 0 : head.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CatchBlock other = (CatchBlock) obj;
        if (exceptions == null) {
            if (other.exceptions != null)
                return false;
        } else if (!Arrays.equals(exceptions, other.exceptions))
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (head == null) {
            if (other.head != null)
                return false;
        } else if (!head.equals(other.head))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "catch (" + StringUtils.join(exceptions, ", ") + ") " + identifier;
    }    
}
