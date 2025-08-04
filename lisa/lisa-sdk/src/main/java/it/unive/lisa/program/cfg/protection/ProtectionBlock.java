package it.unive.lisa.program.cfg.protection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;

public class ProtectionBlock {

    private final Collection<Statement> tryBlock;

    private final List<CatchBlock> catchBlocks;

    private final Collection<Statement> elseBlock;

    private final Collection<Statement> finallyBlock;

    private final Statement tryHead;
    
    private final Statement elseHead;
    
    private final Statement finallyHead;

    public ProtectionBlock(
            Statement tryHead, 
            Collection<Statement> tryBlock, 
            List<CatchBlock> catchBlocks,
            Statement elseHead,
            Collection<Statement> elseBlock, 
            Statement finallyHead,
            Collection<Statement> finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchBlocks = catchBlocks;
        this.elseBlock = elseBlock;
        this.finallyBlock = finallyBlock;
        this.tryHead = tryHead;
        this.elseHead = elseHead;
        this.finallyHead = finallyHead;
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

    public Statement getTryHead() {
        return tryHead;
    }

    public Statement getElseHead() {
        return elseHead;
    }

    public Statement getFinallyHead() {
        return finallyHead;
    }

    public void simplify() {
        tryBlock.removeIf(NoOp.class::isInstance);
        for (CatchBlock cb : catchBlocks)
            cb.simplify();
        if (elseBlock != null && !elseBlock.isEmpty())
            elseBlock.removeIf(NoOp.class::isInstance);
        if (finallyBlock != null && !finallyBlock.isEmpty())
            finallyBlock.removeIf(NoOp.class::isInstance);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tryBlock == null) ? 0 : tryBlock.hashCode());
        result = prime * result + ((catchBlocks == null) ? 0 : catchBlocks.hashCode());
        result = prime * result + ((elseBlock == null) ? 0 : elseBlock.hashCode());
        result = prime * result + ((finallyBlock == null) ? 0 : finallyBlock.hashCode());
        result = prime * result + ((tryHead == null) ? 0 : tryHead.hashCode());
        result = prime * result + ((elseHead == null) ? 0 : elseHead.hashCode());
        result = prime * result + ((finallyHead == null) ? 0 : finallyHead.hashCode());
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
        if (tryHead == null) {
            if (other.tryHead != null)
                return false;
        } else if (!tryHead.equals(other.tryHead))
            return false;
        if (elseHead == null) {
            if (other.elseHead != null)
                return false;
        } else if (!elseHead.equals(other.elseHead))
            return false;
        if (finallyHead == null) {
            if (other.finallyHead != null)
                return false;
        } else if (!finallyHead.equals(other.finallyHead))
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
