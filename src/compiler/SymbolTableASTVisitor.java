package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void, VoidException> {

    private List<Map<String, STentry>> symTable = new ArrayList<>();
    private Map<String, Map<String, STentry>> classTable = new HashMap<>();
    private int nestingLevel = 0; // current nesting level
    int stErrors = 0;
    private int decOffset = -2; // counter for offset of local declarations at current nesting level

    SymbolTableASTVisitor() {
    }

    SymbolTableASTVisitor(boolean debug) {
        super(debug);
    } // enables print for debugging

    private STentry stLookup(String id) {
        int j = nestingLevel;
        STentry entry = null;
        while (j >= 0 && entry == null)
            entry = symTable.get(j--).get(id);
        return entry;
    }

    @Override
    public Void visitNode(ProgLetInNode n) {
        if (print) printNode(n);
        Map<String, STentry> hm = new HashMap<>();
        symTable.add(hm);
        for (Node dec : n.declist) visit(dec);
        visit(n.exp);
        symTable.remove(0);
        return null;
    }

    @Override
    public Void visitNode(ProgNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        if (print) printNode(n);
        Map<String, STentry> hm = symTable.get(nestingLevel);
        List<TypeNode> parTypes = new ArrayList<>();
        for (ParNode par : n.parlist) parTypes.add(par.getType());
        STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.getType()), decOffset--);
        //inserimento di ID nella symtable
        if (hm.put(n.id, entry) != null) {
            System.out.println("Fun id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }
        //creare una nuova hashmap per la symTable, entro nel corpo della funzione
        nestingLevel++;
        Map<String, STentry> hmn = new HashMap<>();
        symTable.add(hmn);

        int prevNLDecOffset = decOffset; // stores counter for offset of declarations at previous nesting level
        decOffset = -2;//dichiarazioni interne all'offset, dichiarazione annidata

        int parOffset = 1;
        for (ParNode par : n.parlist)
            if (hmn.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
                System.out.println("Par id " + par.id + " at line " + n.getLine() + " already declared");
                stErrors++;
            }
        for (Node dec : n.declist) visit(dec);
        visit(n.exp);
        //rimuovere la hashmap corrente poiche' esco dallo scope
        symTable.remove(nestingLevel--);

        decOffset = prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        if (print) printNode(n);
        visit(n.exp);
        Map<String, STentry> hm = symTable.get(nestingLevel);
        STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);
        //inserimento di ID nella symtable
        if (hm.put(n.id, entry) != null) {
            System.out.println("Var id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }
        return null;
    }

    @Override
    public Void visitNode(PrintNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        if (print) printNode(n);
        visit(n.cond);
        visit(n.th);
        visit(n.el);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(TimesNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode n) {
        if (print) printNode(n);
        STentry entry = stLookup(n.id);
        if (entry == null) {
            System.out.println("Fun id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = entry;
            n.nl = nestingLevel;
        }
        for (Node arg : n.arglist) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(IdNode n) {
        if (print) printNode(n);
        STentry entry = stLookup(n.id);
        if (entry == null) {
            System.out.println("Var or Par id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = entry;
            n.nl = nestingLevel;//mi serve capire dove sono, che devo fare la differenza di nesting level per trovare la funzione(il suo indirizzo) vhe voglio usare
        }
        return null;
    }

    //	n.nl = nestingLevel; //
    @Override
    public Void visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return null;
    }

    /*TODO Continuare da qui*/
    @Override
    public Void visitNode(GreaterEqualNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        if (print) printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }


    @Override
    public Void visitNode(ClassNode n) {
        if (print) printNode(n);
        Map<String, STentry> currentLevelMap = symTable.get(nestingLevel);

        //  Costruzione iniziale di ClassTypeNode (con liste vuote)
        ArrayList<ArrowTypeNode> methodTypes = new ArrayList<>();
        ArrayList<TypeNode> fieldTypes = new ArrayList<>();
        //  Gestione campi (solo raccolta dei tipi per ClassTypeNode)
        for (FieldNode f : n.fields) {
            fieldTypes.add(f.getType());  // solo per classType, niente STentry ora
        }
        ClassTypeNode classType = new ClassTypeNode(methodTypes, fieldTypes, n.id);
        STentry classEntry = new STentry(nestingLevel, classType, decOffset--);

        //  Inserimento classe nella symtable
        if (currentLevelMap.put(n.id, classEntry) != null) {
            System.out.println("Class id " + n.id + " already declared");
            stErrors++;
        }

        //  Creazione virtual table per la classe
        Map<String, STentry> classScope = new HashMap<>();
        classTable.put(n.id, classScope);

        // Entrata nello scope della classe
        symTable.add(classScope);
        nestingLevel++;

        // Gestione dei campi: inserimento nella virtual table
        for (FieldNode f : n.fields) {
            if (classScope.put(f.fieldId, new STentry(nestingLevel, f.getType(), decOffset--)) != null) {
                System.out.println("Field id " + f.fieldId + " already declared in class " + n.id);
                stErrors++;
            }
        }

        //  Gestione dei metodi
        for (MethodNode m : n.methods) {
            // Costruzione lista tipi dei parametri
            List<TypeNode> parTypes = new ArrayList<>();
            for (ParNode par : m.parameters) {
                parTypes.add(par.getType());
            }

            ArrowTypeNode methodType = new ArrowTypeNode(parTypes, m.getType());
            methodTypes.add(methodType);

            STentry methodEntry = new STentry(nestingLevel, methodType, decOffset--);

            if (classScope.put(m.id, methodEntry) != null) {
                System.out.println("Method id " + m.id + " already declared in class " + n.id);
                stErrors++;
            }

            // Simula corpo metodo come funzione
            nestingLevel++;
            Map<String, STentry> methodSymTable = new HashMap<>();
            symTable.add(methodSymTable);

            int prevDecOffset = decOffset;
            decOffset = -2;

            int parOffset = 1;
            for (ParNode par : m.parameters) {
                if (methodSymTable.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
                    System.out.println("Param id " + par.id + " already declared in method " + m.id);
                    stErrors++;
                }
            }

            for (Node dec : m.declist) {
                visit(dec);
            }

            visit(m.exp);

            symTable.remove(nestingLevel--);
            decOffset = prevDecOffset;
        }
        // Uscita dallo scope della classe
        symTable.remove(nestingLevel--);

        return null;
    }


    /*@Override
    public Void visitNode(FieldNode n) {

        return null;
    }

    @Override
    public Void visitNode(MethodNode n) {

        return null;
    }*/

    @Override
    public Void visitNode(ClassCallNode n) {
        if (print) printNode(n);
        // Recupera la virtual table
        Map<String, STentry> classScope = classTable.get(n.className);
        if (classScope == null) {
            System.out.println("Class " + n.className + " not declared");
            stErrors++;
            return null;
        }

        // Recupera l'entry del metodo dalla virtual table
        STentry entry = classScope.get(n.methodName);
        if (entry == null) {
            System.out.println("Method " + n.methodName + " not found in class " + n.className);
            stErrors++;
            return null;
        }

        // Salva l'entry del metodo nel nodo per i passi successivi (type checking / code gen)
        n.methodEntry = entry;

        // Visita tutti gli argomenti della chiamata
        for (Node arg : n.args) {
            visit(arg);
        }

        return null;
    }

    @Override
    public Void visitNode(NewNode n) {
        if (print) printNode(n);
        STentry entry = stLookup(n.className);
        if (entry == null) {
            System.out.println("Class id " + n.className + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.classEntry = entry;
        }
        for (Node arg : n.args) visit(arg);
        return null;
    }


    @Override
    public Void visitNode(EmptyNode n) {
        if (print) printNode(n);
        return null;
    }

}







