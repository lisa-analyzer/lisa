package it.unive.lisa.symbolic.value;

import it.unive.lisa.program.cfg.statement.CFGCall;

import java.util.Objects;

/**
 * An identifier outside the current scope of the call, that is, in a method that is in the
 * call stack but not the last one
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public class OutsideScopeIdentifier extends Identifier {
    private CFGCall scope;
    private Identifier id;


    /**
     * Builds the identifier outside the scope.
     *
     * @param id the current identifier
     * @param scope the method call that caused the identifier to exit the scope
     */
    public OutsideScopeIdentifier(Identifier id, CFGCall scope) {
        super(id.getTypes(), scope.getQualifiedName()+":"+id.getName(), id.isWeak());
        this.id = id;
        this.scope = scope;
    }

    /**
     * Returns the identifier hidden in the current scope
     * @return the inner identifier
     */
    public Identifier popScope() {
        return id;
    }

    /**
     * Returns the called method that hidden the identifier
     * @return the hiding called method
     */
    public CFGCall getScope() {
        return scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OutsideScopeIdentifier that = (OutsideScopeIdentifier) o;
        return Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), scope);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
