package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;

import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

    String indent;
    public boolean print;

    ASTGenerationSTVisitor() {
    }

    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }

    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        /*System.out.println("Context class: " + ctxClass.getName());
        System.out.println("Parent class: " + parentClass.getName());*/
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    @Override
    public Node visit(ParseTree t) {
        if (t == null) return null;
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    @Override
    public Node visitProg(ProgContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.progbody());
    }

    @Override
    public Node visitLetInProg(LetInProgContext c) {
        if (print) printVarAndProdName(c);

        List<Node> declist = new ArrayList<>();

        // Visit class declarations first
        for (CldecContext cldec : c.cldec()) {
            System.out.println("Class type: " + cldec.getClass().getSimpleName());
            declist.add(visit(cldec));
        }

        // Then visit regular declarations
        for (DecContext dec : c.dec()) {
            System.out.println("Declaration type: " + dec.getClass().getSimpleName());
            declist.add(visit(dec));
        }

        return new ProgLetInNode(declist, visit(c.exp()));
    }


    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) printVarAndProdName(c);
        return new ProgNode(visit(c.exp()));
    }

    /*TODO GESTIRE MOLTIPLICAZIONE DIVISIONE*/
    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) printVarAndProdName(c);
        Node n;
        if (c.TIMES() != null) {
            n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.TIMES().getSymbol().getLine());  // Set line number
        } else if (c.DIV() != null) {
            n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.DIV().getSymbol().getLine());  // Set line number
        } else {
            throw new IllegalArgumentException("Unexpected operator in TimesDivContext");
        }
        return n;
    }

    /*TODO GESTIRE SOMMA DIFFERENZA*/
    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) printVarAndProdName(c);
        Node n;
        if (c.PLUS() != null) {
            n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.PLUS().getSymbol().getLine());
        } else if (c.MINUS() != null) {
            n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.MINUS().getSymbol().getLine());
        } else {
            throw new IllegalArgumentException("Unexpected operator in PlusMinusContext");
        }

        return n;
    }

    /*TODO GESTIRE CONDIZIONI*/
    @Override
    public Node visitComp(CompContext c) {
        if (print) printVarAndProdName(c);
        Node n;
        if (c.EQ() != null) {
            n = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.EQ().getSymbol().getLine());

        } else if (c.GE() != null) {
            n = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.GE().getSymbol().getLine());

        } else if (c.LE() != null) {
            n = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.LE().getSymbol().getLine());

        } else {
            throw new IllegalArgumentException("Unexpected operator in CompContext");
        }
        return n;
    }

    @Override
    public Node visitCldec(CldecContext c) {
        if (print) printVarAndProdName(c);

        String classId = c.ID(0).getText(); // Class name

        ArrayList<FieldNode> fieldNodes = new ArrayList<>();
        ArrayList<TypeNode> fieldTypes = new ArrayList<>();

        int idIndex = 1;
        for (int i = 0; i < c.type().size(); i++) {
            String fieldName = c.ID(idIndex + i).getText();
            TypeNode fieldType = (TypeNode) visit(c.type(i));
            FieldNode field = new FieldNode(fieldName, fieldType);
            field.setLine(c.ID(idIndex + i).getSymbol().getLine());
            fieldNodes.add(field);
            fieldTypes.add(fieldType);
        }

        ArrayList<MethodNode> methodNodes = new ArrayList<>();
        ArrayList<ArrowTypeNode> methodTypes = new ArrayList<>();
        for (MethdecContext m : c.methdec()) {
            MethodNode method = (MethodNode) visit(m);
            method.classId = classId; // <- Set classId here
            methodNodes.add(method);
            methodTypes.add((ArrowTypeNode) method.getType()); // Cast safely assuming correct return
        }

        ClassTypeNode classType = new ClassTypeNode(methodTypes, fieldTypes, classId);
        ClassNode classNode = new ClassNode(classId, classType, fieldNodes, methodNodes);
        classNode.setLine(c.CLASS().getSymbol().getLine());

        return classNode;
    }

    @Override
    public Node visitMethdec(MethdecContext c) {
        if (print) printVarAndProdName(c);

        String methodName = c.ID(0).getText();
        TypeNode returnType = (TypeNode) visit(c.type(0));

        List<ParNode> parameters = new ArrayList<>();
        ArrayList<TypeNode> paramTypes = new ArrayList<>();

        for (int i = 1; i < c.ID().size(); i++) {
            String paramName = c.ID(i).getText();
            TypeNode paramType = (TypeNode) visit(c.type(i));
            paramTypes.add(paramType);
            ParNode param = new ParNode(paramName, paramType);
            param.setLine(c.ID(i).getSymbol().getLine());
            parameters.add(param);
        }

        List<DecNode> decList = new ArrayList<>();
        if (c.dec() != null) {
            for (DecContext dec : c.dec()) {
                decList.add((DecNode) visit(dec));
            }
        }

        Node body = visit(c.exp());
        ArrowTypeNode methodType = new ArrowTypeNode(paramTypes, returnType);
        MethodNode methodNode = new MethodNode(methodName, methodType, parameters, decList, body);
        //System.out.println("MethContext is returning " + ((ArrowTypeNode) methodNode.getType()).ret);
        methodNode.setLine(c.FUN().getSymbol().getLine());

        return methodNode;
    }

    @Override
    public Node visitNew(NewContext c) {
        if (print) printVarAndProdName(c);
        Node n = null;
        List<Node> argList = new ArrayList<>();
        if (c.exp() != null && !c.exp().isEmpty()) {
            for (ParseTree e : c.exp()) {
                argList.add(visit(e));
            }
        }
        n = new NewNode(c.ID().getSymbol().getText(), argList);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }


    @Override
    public Node visitVardec(VardecContext c) {
        if (print) printVarAndProdName(c);
        Node n = null;
        if (c.ID() != null) {
            TypeNode declaredType = (TypeNode) visit(c.type());
            Node expNode = visit(c.exp());
            n = new VarNode(c.ID().getText(), declaredType, expNode);
            n.setLine(c.VAR().getSymbol().getLine());
        }
        return n;
    }


    @Override
    public Node visitFundec(FundecContext c) {
        if (print) printVarAndProdName(c);
        ArrayList<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }
        ArrayList<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
        Node n = null;
        if (!c.ID().isEmpty()) { //non-incomplete ST
            n = new FunNode(c.ID(0).getText(), (TypeNode) visit(c.type(0)), parList, decList, visit(c.exp()));
            n.setLine(c.FUN().getSymbol().getLine());
        }
        return n;
    }

    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) printVarAndProdName(c);
        return new IntTypeNode();
    }

    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) printVarAndProdName(c);
        return new BoolTypeNode();
    }

    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) printVarAndProdName(c);
        int v = Integer.parseInt(c.NUM().getText());
        return new IntNode(c.MINUS() == null ? v : -v);
    }

    @Override
    public Node visitTrue(TrueContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(true);
    }

    @Override
    public Node visitFalse(FalseContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(false);
    }

    @Override
    public Node visitIf(IfContext c) {
        if (print) printVarAndProdName(c);
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));
        Node n = new IfNode(ifNode, thenNode, elseNode);
        n.setLine(c.IF().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitPrint(PrintContext c) {
        if (print) printVarAndProdName(c);
        return new PrintNode(visit(c.exp()));
    }

    @Override
    public Node visitPars(ParsContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.exp());
    }

    @Override
    public Node visitNot(NotContext c) {
        if (print) printVarAndProdName(c);
        return new NotNode(visit(c.exp()));
    }

    @Override
    public Node visitId(IdContext c) {
        if (print) printVarAndProdName(c);
        System.out.println("Parsed as ID: " + c.getText());
        Node n = new IdNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }


    @Override
    public Node visitIdType(IdTypeContext c) {
        if (print) printVarAndProdName(c);
        Node n = new RefTypeNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitCall(CallContext c) {
        if (print) printVarAndProdName(c);
        System.out.println("Parsed as Call: " + c.getText());
        ArrayList<Node> parlist = new ArrayList<>();
        for (ExpContext arg : c.exp()) parlist.add(visit(arg));
        Node n = new CallNode(c.ID().getText(), parlist);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitNull(NullContext ctx) {
        if (print) printVarAndProdName(ctx);
        return new EmptyNode();
    }

    @Override
    public Node visitDotCall(DotCallContext ctx) {
        if (print) printVarAndProdName(ctx);
        // First ID: object variable name
        String objId = ctx.ID(0).getText();

        // Second ID: method name
        String methodId = ctx.ID(1).getText();

        // Arguments: zero or more expressions inside parentheses
        List<Node> args = new ArrayList<>();
        for (ExpContext expCtx : ctx.exp()) {
            args.add(visit(expCtx));
        }
        Node n = new ClassCallNode(objId, methodId, args);
        n.setLine(ctx.ID().getFirst().getSymbol().getLine());
        // Create and return the AST node representing this dot-call
        return n;
    }

}
