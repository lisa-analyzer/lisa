package it.unive.lisa.program.cfg.protection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.cfg.statement.Statement;

public class ProtectionBlock {

    private final Collection<Statement> tryBlock;

    private final List<CatchBlock> catchBlocks;

    private final Collection<Statement> elseBlock;

    private final Collection<Statement> finallyBlock;

    public ProtectionBlock(Collection<Statement> tryBlock, List<CatchBlock> catchBlocks,
            Collection<Statement> elseBlock, Collection<Statement> finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchBlocks = catchBlocks;
        this.elseBlock = elseBlock;
        this.finallyBlock = finallyBlock;
    }

    public Collection<Statement> getTryBlock() {
        return tryBlock;
    }

    public List<CatchBlock> getCatchBlocks() {
        return catchBlocks;
    }

    public Collection<Statement> getElseBlock() {
        return elseBlock;
    }

    public Collection<Statement> getFinallyBlock() {
        return finallyBlock;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tryBlock == null) ? 0 : tryBlock.hashCode());
        result = prime * result + ((catchBlocks == null) ? 0 : catchBlocks.hashCode());
        result = prime * result + ((elseBlock == null) ? 0 : elseBlock.hashCode());
        result = prime * result + ((finallyBlock == null) ? 0 : finallyBlock.hashCode());
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
        ProtectionBlock other = (ProtectionBlock) obj;
        if (tryBlock == null) {
            if (other.tryBlock != null)
                return false;
        } else if (!tryBlock.equals(other.tryBlock))
            return false;
        if (catchBlocks == null) {
            if (other.catchBlocks != null)
                return false;
        } else if (!catchBlocks.equals(other.catchBlocks))
            return false;
        if (elseBlock == null) {
            if (other.elseBlock != null)
                return false;
        } else if (!elseBlock.equals(other.elseBlock))
            return false;
        if (finallyBlock == null) {
            if (other.finallyBlock != null)
                return false;
        } else if (!finallyBlock.equals(other.finallyBlock))
            return false;
        return true;
    }

    @Override
    public String toString() {
        Collection<String> components = new LinkedList<>();
        if (elseBlock != null)
            components.add("else");
        if (finallyBlock != null)
            components.add("finally");
        String comp = StringUtils.join(components, "-");
        if (!comp.isEmpty())
            comp = "-" + comp;
        return "ProtectionBlock [try" + comp +  ", " + StringUtils.join(catchBlocks, ", ") + "]";
    }

    

}
