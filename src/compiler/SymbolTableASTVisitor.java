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
        ArrayList<TypeNode> parTypes = new ArrayList<>();
        for (ParNode par : n.parlist) parTypes.add(par.getType());
        STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.getType()), decOffset--);
        n.setSymType(entry.type);
        System.out.println("FUNC ARROWTYPEMETHOD vs GETTYPE " + n.id + " " + parTypes + "  " + n.getType() + " entry type" + entry.type);
        if (nestingLevel == 0) {
            decOffset--;
        }
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
        for (DecNode dec : n.declist) visit(dec);
        visit(n.exp);//rimuovere la hashmap corrente poiche' esco dallo scope
        symTable.remove(nestingLevel--);

        decOffset = prevNLDecOffset; // restores counter for offset of declarations at previous nesting level

        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        if (print) printNode(n);
        visit(n.getType());
        visit(n.exp);
        Map<String, STentry> hm = symTable.get(nestingLevel);
        System.out.println("Tipo di newNode " + n.exp);
        STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);

        var dec = decOffset + 1;
        System.out.println("Dichiarazione della var " + n.id + " exp : " + n.exp + " " + entry.nl + " vs decOffset --" + dec + " " + n.getType() + " " + entry.offset + "entry type " + entry.type);
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
        for (Node arg : n.parlist) visit(arg);
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
            System.out.println("Resolved id " + n.id + " to offset " + entry.offset + " at nesting level " + nestingLevel);
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
        if (print) printNode(n, n.exp.toString());
        visit(n.exp);
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
        if (print) printNode(n, n.id);
        Map<String, STentry> currentLevelMap = symTable.get(nestingLevel);

        //  Costruzione iniziale di ClassTypeNode (con liste vuote)
        ArrayList<ArrowTypeNode> methodTypes = new ArrayList<>(Collections.nCopies(n.methods.size(), null));
        ArrayList<TypeNode> fieldTypes = new ArrayList<>(Collections.nCopies(n.fields.size(), null));

        ClassTypeNode classType = new ClassTypeNode(methodTypes, fieldTypes, n.id);
        STentry classEntry = new STentry(nestingLevel, classType, decOffset--);

        var dec = decOffset + 1;
        System.out.println("Dichiarazione della classe  " + n.id + " " + classEntry.nl + " vs decOffset " + dec + " " + n.getType() + " " + classEntry.offset + "entry type " + classEntry.type);

        //  Inserimento classe nella symtable
        if (currentLevelMap.put(n.id, classEntry) != null) {
            System.out.println("Class id " + n.id + " already declared");
            stErrors++;
        }

        //  Creazione virtual table per la classe
        Map<String, STentry> classScope = new HashMap<>();
        classTable.put(n.id, classScope);
        System.out.println("ID DELLA CLASSE " + n.id + " IL SUO CLASSTABLE " + classScope.get(n.id));

        // Entrata nello scope della classe
        symTable.add(classScope);
        nestingLevel++;

        int fieldOffset = -1; // Negative offsets for fields TODO in teoria va bene
        for (int i = 0; i < n.fields.size(); i++) {
            FieldNode f = n.fields.get(i);
            f.offset = fieldOffset;
            classType.allFields.set(i, f.getType()); // Fill ClassTypeNode's field types by index

            STentry entry = new STentry(nestingLevel, f.getType(), fieldOffset--);
            if (classScope.put(f.fieldId, entry) != null) {
                System.out.println("Field id " + f.fieldId + " already declared in class " + n.id);
                stErrors++;
            }
        }

        int methodOffset = 0;
        //  Gestione dei metodi
        for (MethodNode m : n.methods) {
            m.classId = n.id;
            m.offset = methodOffset++;
            visit(m);          // visitNode(MethodNode)
        }
        //rimuovere la hashmap corrente poiche' esco dallo scope
        symTable.remove(nestingLevel--);

        System.out.println("symTable keys at first level: " + symTable.getFirst().keySet());
        System.out.println("ID DELLA CLASSE NUOVA STAMPA DOPO AVER AGGIUNTO I METODI " + n.id + " IL SUO CLASSTABLE " + classTable.get(n.id));

        System.out.println("Contenuto della classScope per la classe " + n.id + ":");
        Map<String, STentry> classScopeDump = classTable.get(n.id);
        for (Map.Entry<String, STentry> e : classScopeDump.entrySet()) {
            System.out.println("  " + e.getKey() + " -> offset: " + e.getValue().offset + ", type: " + e.getValue().type);
        }

        /*if (classScope.containsKey("")) {
            System.out.println("classScope contains method/field: " + key + " -> " + classScope.get(key));
        } else {
            System.out.println("classScope does NOT contain: " + key);
        }*/

        return null;
    }


    @Override
    public Void visitNode(MethodNode n) {
        if (print) printNode(n);

        // Recupera virtual table (classScope) e classTypeNode
        Map<String, STentry> classScope = classTable.get(n.classId);

        STentry classEntry = null;
        for (int i = nestingLevel - 1; i >= 0 && classEntry == null; i--) {
            classEntry = symTable.get(i).get(n.classId);
        }
        if (classEntry == null) {
            System.out.println("Class entry not found for " + n.classId);
            stErrors++;
            return null;
        }

        ClassTypeNode classType = (ClassTypeNode) classEntry.type;

        // Costruisci tipo freccia del metodo
        ArrayList<TypeNode> parTypes = new ArrayList<>();
        for (ParNode p : n.parameters) {
            parTypes.add(p.getType());
        }
        ArrowTypeNode methodType = new ArrowTypeNode(parTypes, n.getType());
        System.out.println("STO METTENDO RET IN METHODENTRY  " + methodType + " asd  " + methodType.ret + " type originale " + n.getType());
        // Aggiorna virtual table
        STentry methodEntry = new STentry(nestingLevel, methodType, n.offset);
        if (classScope.put(n.id, methodEntry) != null) {
            System.out.println("Method id " + n.id + " already declared in class " + n.classId);
            stErrors++;
        }
        System.out.println("Dichiarazione del method " + n.id + " exp : " + n.exp + " " + methodEntry.nl + " vs method offset  " + n.offset + " " + n.getType() + " " + methodEntry.offset + "entry type " + methodEntry.type);

        // Aggiorna ClassTypeNode
        classType.allMethods.set(n.offset, methodType);

        // Simula corpo metodo
        nestingLevel++;
        Map<String, STentry> methodSymTable = new HashMap<>();
        symTable.add(methodSymTable);

        int prevDecOffset = decOffset;
        decOffset = -2;// reset decOffset solo per parametri e variabili locali

        int parOffset = 1;
        for (ParNode p : n.parameters) {
            if (methodSymTable.put(p.id, new STentry(nestingLevel, p.getType(), parOffset++)) != null) {
                System.out.println("Param id " + p.id + " already declared in method " + n.id);
                stErrors++;
            }
        }

        for (DecNode d : n.declist) visit(d);
        visit(n.exp);

        symTable.remove(nestingLevel--);
        decOffset = prevDecOffset;

        return null;
    }


    @Override
    public Void visitNode(ClassCallNode n) {
        if (print) printNode(n);
        // Cerca l'entry di ID1 (oggetto)
        STentry objectEntry = null;
        for (int i = nestingLevel; i >= 0 && objectEntry == null; i--) {
            objectEntry = symTable.get(i).get(n.className);
        }
        if (objectEntry == null) {
            System.out.println("Identifier " + n.className + " not declared");
            stErrors++;
            return null;
        }
        n.classEntry = objectEntry;

        // Verifica che ID1 abbia tipo RefTypeNode (cioè è un oggetto)
        if (!(objectEntry.type instanceof RefTypeNode refType)) {
            System.out.println("Cannot call method on non-object type: " + n.className);
            stErrors++;
            return null;
        }
        // Recupera la virtual table della classe
        Map<String, STentry> classScope = classTable.get(refType.className);
        if (classScope == null) {
            System.out.println("Class " + n.className + " not declared");
            stErrors++;
            return null;
        }

        // Recupera l'entry del metodo ID2 dalla virtual table
        STentry methodEntry = classScope.get(n.methodName);
        if (methodEntry == null) {
            System.out.println("Method " + n.methodName + " not found in class " + n.className);
            stErrors++;
            return null;
        }
        System.out.println("CHECK METHODENTRYYYY TYPE" + methodEntry.type);
        n.methodEntry = methodEntry;
        n.nl = nestingLevel;

        // Visita gli argomenti
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
            n.nestingLevel = nestingLevel;
            n.setType(entry.type);
            System.out.println("Class id " + n.className + " at line " + n.getLine() + " declared" + "type is " + n.classEntry.type);
            System.out.println("NEWNODE IS " + n.className + " " + n.type);
        }
        for (Node arg : n.args) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode n) {
        if (print) printNode(n, n.className);

        // Only top-level scope contains class definitions
        STentry classEntry = stLookup(n.className);

        if (classEntry == null || !(classEntry.type instanceof ClassTypeNode)) {
            System.out.println("Class " + n.className + " not declared (used as type)");
            stErrors++;
        } else {
            System.out.println("RefType " + n.className + " correctly resolved to class type");
        }

        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        if (print) printNode(n, n.toString());
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode n) {
        if (print) printNode(n);
        return null;
    }

}







