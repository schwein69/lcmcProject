package compiler.lib;

import compiler.AST;

public abstract class DecNode extends Node {
    protected TypeNode type;

    public DecNode(TypeNode t) {
        this.type = t;
    }

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }
}
