package compiler.lib;

public abstract class DecNode extends Node {
    protected TypeNode type;

    public DecNode(TypeNode t) {
        this.type = t;
    }

    public TypeNode getType() {
        return type;
    }
}
