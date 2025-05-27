package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void, VoidException> {

    PrintEASTVisitor() {
        super(false, true);
    }

    @Override
    public Void visitNode(ProgLetInNode n) {
        printNode(n);
        for (Node dec : n.declist) visit(dec);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ProgNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        printNode(n, n.id);
        visit(n.getType());
        for (ParNode par : n.parlist) visit(par);
        for (Node dec : n.declist) visit(dec);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ParNode n) {
        printNode(n, n.id);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        printNode(n, n.id);
        visit(n.getType());
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(PrintNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        printNode(n);
        visit(n.cond);
        visit(n.th);
        visit(n.el);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(TimesNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nl);
        visit(n.entry);
        for (Node arg : n.arglist) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(IdNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nl);
        visit(n.entry);
        return null;
    }

    @Override
    public Void visitNode(BoolNode n) {
        printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(ArrowTypeNode n) {
        printNode(n);
        for (Node par : n.parlist) visit(par);
        visit(n.ret, "->"); //marks return type
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode n) {
        printNode(n);
        return null;
    }

    //TODO ADD NEW METHODS HERE
    @Override
    public Void visitNode(GreaterEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    //TODO object oriented
    @Override
    public Void visitNode(ClassNode n) {
        printNode(n);
        for (FieldNode field : n.fields) {
            visit(field);
        }
        for (MethodNode method : n.methods) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visitNode(FieldNode n) {
        printNode(n);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(MethodNode n) {
        printNode(n, n.id);
        visit(n.getType());
        for (ParNode param : n.parameters) {
            visit(param);
        }
        for (Node stmt : n.declist) {
            visit(stmt);
        }
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode n) {
        printNode(n, n.className);
        visit(n.methodEntry);
        for (Node arg : n.args) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(NewNode n) {
        printNode(n, n.className);
        visit(n.classEntry);
        for (Node arg : n.args) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        return null;
    }

    @Override
    public Void visitNode(ClassTypeNode n) {
        printNode(n);
        for (TypeNode fieldType : n.allFields) visit(fieldType);
        for (TypeNode methodType : n.allMethods) visit(methodType);
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(EmptyTypeNode n) {
        printNode(n, "empty node");
        return null;
    }

    @Override
    public Void visitSTentry(STentry entry) {
        printSTentry("nestlev " + entry.nl);
        printSTentry("type");
        printSTentry("offset:" + entry.offset);
        visit(entry.type);
        return null;
    }

}
