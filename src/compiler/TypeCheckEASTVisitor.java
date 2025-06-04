package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import java.util.ArrayList;

import static compiler.lib.FOOLlib.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode, TypeException> {

    TypeCheckEASTVisitor() {
        super(true);
    } // enables incomplete tree exceptions

    TypeCheckEASTVisitor(boolean debug) {
        super(true, debug);
    } // enables print for debugging

    //checks that a type object is visitable (not incomplete)
    private TypeNode ckvisit(TypeNode t) throws TypeException {
        visit(t);
        return t;
    }

    @Override
    public TypeNode visitNode(ProgLetInNode n) throws TypeException {
        if (print) printNode(n);
        for (Node dec : n.declist)
            try {
                visit(dec);
            } catch (IncomplException e) {
                System.out.println("Type checking error in : " + dec.getLine());
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(ProgNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(FunNode n) throws TypeException {
        if (print) printNode(n, n.id);
        for (Node dec : n.declist)
            try {
                visit(dec);
            } catch (IncomplException e) {
                System.out.println("In Function Type checking error in a function incomplException: " + dec);
            } catch (TypeException e) {
                System.out.println("In Function Type checking error in a declaration: " + e.text);
            }
        if (!isSubtype(visit(n.exp), n.getType())) //check
            throw new TypeException("Wrong return type for function " + n.id, n.getLine());
        return null;
    }

    @Override
    public TypeNode visitNode(VarNode n) throws TypeException {
        if (print) printNode(n, n.id);
        if (!isSubtype(visit(n.exp), ckvisit(n.getType()))) //check
            throw new TypeException("Incompatible value for variable " + n.id, n.getLine());
        return null;
    }

    @Override
    public TypeNode visitNode(PrintNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(IfNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.cond), new BoolTypeNode())))
            throw new TypeException("Non boolean condition in if", n.getLine());
        TypeNode t = visit(n.th);
        TypeNode e = visit(n.el);
        if (t instanceof ArrowTypeNode innerl) {
            t = innerl.ret;
        }
        if (e instanceof ArrowTypeNode innerr) {
            e = innerr.ret;
        }
        if (isSubtype(t, e)) return e;
        if (isSubtype(e, t)) return t;
        throw new TypeException("Incompatible types in then-else branches" + " th " + t + " e " + e, n.getLine());
    }

    @Override
    public TypeNode visitNode(EqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (l instanceof ArrowTypeNode || r instanceof ArrowTypeNode) {
            System.out.println("Functional type not allowed in equal");
            System.exit(0);
        }
        if (!(isSubtype(l, r) || isSubtype(r, l)))
            throw new TypeException("Incompatible types in equal", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(TimesNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in multiplication", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(PlusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in sum", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(CallNode n) throws TypeException {
        if (print) printNode(n, n.id);
        TypeNode rawType = visit(n.entry);
        if (!(rawType instanceof ArrowTypeNode)) {
            throw new TypeException("Invocation of a non-function " + n.id, n.getLine());
        }
        ArrowTypeNode t = (ArrowTypeNode) rawType;
        //System.out.println("IN CALL NODE STAMPO ENTRY TYPE " + t.ret + " STAMPO PARAMTYPE" + t.parlist);
        //System.out.println("IN CALL NODE STAMPO PARTYPE DEL CALLNODE :  " + visit(n.parlist.getFirst()));
        if (t == null)
            throw new TypeException("Invocation of a non-function " + n.id, n.getLine());
        ArrayList<TypeNode> p = t.parlist;
        if (!(p.size() == n.parlist.size()))
            throw new TypeException("Wrong number of parameters in the invocation of " + n.id, n.getLine());

        for (int i = 0; i < n.parlist.size(); i++) {
            TypeNode actual = p.get(i);
            TypeNode passed = visit(n.parlist.get(i));
            if (passed instanceof ArrowTypeNode actualArrow) {
                TypeNode innerRetType = actualArrow.ret;
                if (!(FOOLlib.isSubtype(actual, innerRetType))) {
                    throw new TypeException("IN CALL NODE Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.id + " expected: " + p.get(i)
                            + " passed: " + visit(n.parlist.get(i)), n.getLine());
                }
            } else {
                if (!(FOOLlib.isSubtype(actual, passed))) {
                    throw new TypeException("IN CALL NODE Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.id + " expected: " + p.get(i)
                            + " passed: " + visit(n.parlist.get(i)), n.getLine());
                }
            }

        }
        return t.ret;
    }


    //Per le variabili
    @Override
    public TypeNode visitNode(IdNode n) throws TypeException {
        if (print) printNode(n, n.id);
        TypeNode t = visit(n.entry); // STentry visit
        if (t instanceof ClassTypeNode)
            throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
        return t;
    }

    @Override
    public TypeNode visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return new IntTypeNode();
    }

// gestione tipi incompleti	(se lo sono lancia eccezione)

    @Override
    public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (Node par : n.parlist) visit(par);
        visit(n.ret, "->"); //marks return type
        return null;
    }

    @Override
    public TypeNode visitNode(BoolTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(IntTypeNode n) {
        if (print) printNode(n);
        return null;
    }


    @Override
    public TypeNode visitSTentry(STentry entry) throws TypeException {
        if (print) printSTentry("type");
        return ckvisit(entry.type); //check
    }

    // TODO OPERATORS EXTENSION

    @Override
    public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode leftInnerRetType = visit(n.left);
        TypeNode rightInnerRetType = visit(n.right);
        if (leftInnerRetType instanceof ArrowTypeNode actualArrowLeft) {
            leftInnerRetType = actualArrowLeft.ret;
        }
        if (rightInnerRetType instanceof ArrowTypeNode actualArrowRight) {
            rightInnerRetType = actualArrowRight.ret;
        }
        if (!(isSubtype(leftInnerRetType, new IntTypeNode()) && isSubtype(rightInnerRetType, new IntTypeNode()))) {
            throw new TypeException("Non integers in >= operation", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(LessEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode leftInnerRetType = visit(n.left);
        TypeNode rightInnerRetType = visit(n.right);
        if (leftInnerRetType instanceof ArrowTypeNode actualArrowLeft) {
            leftInnerRetType = actualArrowLeft.ret;
        }
        if (rightInnerRetType instanceof ArrowTypeNode actualArrowRight) {
            rightInnerRetType = actualArrowRight.ret;
        }
        //System.out.println("TYPE LEFT " + l + " TYPE RIGHT " + r);
        if (!(isSubtype(leftInnerRetType, new IntTypeNode()) && isSubtype(rightInnerRetType, new IntTypeNode()))) {
            throw new TypeException("Non integers in <= operation", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(NotNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode t = visit(n.exp);
        if (!(isSubtype(t, new BoolTypeNode()))) {
            throw new TypeException("Non boolean in not operation", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(MinusNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, new IntTypeNode()) && isSubtype(r, new IntTypeNode()))) {
            throw new TypeException("Non integers in minus operation", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(OrNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, new BoolTypeNode()) && isSubtype(r, new BoolTypeNode()))) {
            throw new TypeException("Non boolean in or operation", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(DivNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, new IntTypeNode()) && isSubtype(r, new IntTypeNode()))) {
            throw new TypeException("Non integers in division", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(AndNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, new BoolTypeNode()) && isSubtype(r, new BoolTypeNode()))) {
            throw new TypeException("Non boolean in and operation", n.getLine());
        }
        return new BoolTypeNode();
    }

    //TODO OBJECT-ORIENTED EXTENSION

    @Override
    public TypeNode visitNode(ClassNode n) throws TypeException {
        if (print) printNode(n);
        for (MethodNode m : n.methods) {
            visit(m);
        }
        return null;
    }

    @Override
    public TypeNode visitNode(MethodNode n) throws TypeException {
        if (print) printNode(n, n.id);
        visit(n.getType());
        for (Node d : n.declist) {
            try {
                visit(d);
            } catch (IncomplException e) {
                System.out.println("IncomplException in method: " + e.getMessage());
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration of method: " + e.text);
            }
        }
        //System.out.println("method n.getType " + n.getType());
        if (!isSubtype(visit(n.exp), ((ArrowTypeNode) n.getType()).ret)) {
            throw new TypeException("Wrong return type in method " + n.id, n.getLine());
        }
        return null;
    }

    @Override
    public TypeNode visitNode(ClassCallNode n) throws TypeException {
        if (print) printNode(n, n.className);
        TypeNode t = visit(n.methodEntry);
        if (!(t instanceof ArrowTypeNode)) {
            throw new TypeException("Invocation of a non-method " + n.methodEntry, n.getLine());
        }
        ArrowTypeNode at = (ArrowTypeNode) t;
        if (at.parlist.size() != n.args.size()) {
            throw new TypeException("Wrong number of parameters in method call " + n.methodEntry, n.getLine());
        }
        for (int i = 0; i < n.args.size(); i++) {
            if (!isSubtype(visit(n.args.get(i)), at.parlist.get(i))) {
                throw new TypeException("IN CLASSCALLNODE Wrong type for " + (i + 1) + "-th parameter in method call " + n.methodEntry, n.getLine());
            }
        }
        return at.ret;
    }


    @Override
    public TypeNode visitNode(NewNode n) throws TypeException {
        if (print) printNode(n, n.className);
        TypeNode rawType = visit(n.classEntry);
        if (!(rawType instanceof ClassTypeNode)) {
            throw new TypeException("Instantiation of a non-class " + n.className, n.getLine());
        }
        ClassTypeNode ctn = (ClassTypeNode) rawType;
        // Check constructor args match fields
        if (ctn.allFields.size() != n.args.size()) {
            throw new TypeException("Wrong number of parameters in constructor of class " + n.className, n.getLine());
        }
        for (int i = 0; i < n.args.size(); i++) {
            TypeNode actual = ctn.allFields.get(i);
            TypeNode passed = visit(n.args.get(i));
            if (passed instanceof ArrowTypeNode actualArrow) {
                TypeNode innerRetType = actualArrow.ret;
                if (!(FOOLlib.isSubtype(actual, innerRetType))) {
                    throw new TypeException("IN CALL NODE Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.className + " expected: " + ctn.allFields.get(i)
                            + " passed: " + visit(n.args.get(i)), n.getLine());
                }
            } else {
                if (!(FOOLlib.isSubtype(actual, passed))) {
                    throw new TypeException("IN CALL NODE Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.className + " expected: " + ctn.allFields.get(i)
                            + " passed: " + visit(n.args.get(i)), n.getLine());
                }
            }

        }
       /* for (int i = 0; i < n.args.size(); i++) {
            if (!isSubtype(visit(n.args.get(i)), ctn.allFields.get(i))) {
                throw new TypeException("In NEWNODE Wrong type for " + (i + 1) + "-th parameter in constructor of class " + n.className + " expected: " + ctn.allFields.get(i).getClass()
                        + " passed: " + visit(n.args.get(i)).getClass(), n.getLine());
            }
        }*/
        return new RefTypeNode(n.className);
    }

    @Override
    public TypeNode visitNode(EmptyNode n) {
        if (print) printNode(n, n.toString());
        return new EmptyTypeNode();
    }


    @Override
    public TypeNode visitNode(ClassTypeNode n) throws TypeException {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(RefTypeNode n) throws TypeException {
        if (print) printNode(n, "RefTypeNode");
        return null;
    }

    @Override
    public TypeNode visitNode(EmptyTypeNode n) throws TypeException {
        if (print) printNode(n, "empty node");
        return null;
    }

}