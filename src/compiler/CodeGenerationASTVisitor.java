package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

import java.util.ArrayList;
import java.util.Collections;

import static compiler.lib.FOOLlib.*;
import static svm.ExecuteVM.MEMSIZE;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {
    private ArrayList<String> dispatchTable = new ArrayList<>();

    CodeGenerationASTVisitor() {
    }

    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    } //enables print for debugging

    @Override
    public String visitNode(ProgLetInNode n) {
        if (print) printNode(n);
        String declCode = null;
        for (Node dec : n.declist) declCode = nlJoin(declCode, visit(dec));//joino tutti i codici della dichiarazione
//		visit(n.exp);
// generate code for declarations (allocation)
//		return null;
        return nlJoin("push 0", declCode, visit(n.exp), "halt", getCode());//genera codice per dichiarazioni, push 0 fittizio(guarda file),getcode mette le funzioni in fondo(osserva file.fool.asm)
    }// Formato codice generato: o fittizio, dichiarazioni , contenuto centrale, stop, labels delle funzioni

    @Override
    public String visitNode(ProgNode n) {
        if (print) printNode(n);
//		visit(n.exp);
//		return null;
        return nlJoin(visit(n.exp), "halt");
    }

    @Override
    public String visitNode(FunNode n) {
        if (print) printNode(n, n.id);
        for (ParNode par : n.parlist) visit(par); //numero di parametri da rimuovere
        String decCode = null;
        for (int i = n.declist.size() - 1; i >= 0; i--) decCode = nlJoin(decCode, visit(n.declist.get(i)));
        ;//visit(dec);//numero di valori da rimuovere #TODO nella funzione devo disallocare
        String bodyexp = visit(n.exp);
        String funl = freshFunLabel();

        putCode(
                nlJoin(//TODO debbugare per capire
                        funl + ":",       // Function label
                        "cfp",            // Set $fp to $sp value (Control Link setup) , cioè punta allo stack pointer che punta Access link
                        "lra",            // Load $ra value Return address (guarda file), carico ra perchè va messo da parte (altrimenti si incasina con il bodyexp)
                        decCode,         // Code for local declarations, dichiarazioni locali(valori o indirizzi se sono nuove dichiarazioni), they use the $fp
                        bodyexp,      // Code for the function body
                        "stm",            // Store the result in $tm (function result)
                        generateRemoveCode(n.declist.size()), // Remove local declarations from stack (metto a posto la casa prima di saltare indietro, multiple pop per rimuovere)
                        "sra",            // Set $ra to the popped value (risetto return address e poi rimuovo)
                        "pop",            // Remove Access Link (non mi serve più, sono arrivato a offset 0)
                        generateRemoveCode(n.parlist.size()), // Remove parameters from stack
                        "sfp",            // Set $fp to the popped value (Control Link) (ripristino al chiamante) (ho messo a posto la casa)
                        "ltm",            // Load $tm value (carichiamo il risultato)
                        "lra",            // Load $ra value
                        "js"              // Jump to the popped address (js e poi ra vengono eseguiti)
                )
        );

        return "push " + funl;
    }

    @Override
    public String visitNode(MethodNode n) {
        if (print) printNode(n, n.id);

        // Genera una label unica per questo metodo
        String methodLabel = freshFunLabel();
        n.label = methodLabel;  // salva la label nel nodo

        // Visita parametri (serve a sapere quanti pop fare dopo)(prima li pusho)
        for (ParNode par : n.parameters) visit(par);

        // Genera codice dichiarazioni locali (in ordine inverso)
        String decCode = null;
        for (int i = n.declist.size() - 1; i >= 0; i--) {
            decCode = nlJoin(decCode, visit(n.declist.get(i)));
        }

        // Genera codice corpo del metodo
        String bodyCode = visit(n.exp);

        // Montaggio del codice come per FunNode
        String methodCode = nlJoin(
                methodLabel + ":",       // Etichetta del metodo
                "cfp",                   // Salva il frame pointer
                "lra",                   // Salva il return address
                decCode,                 // Codice dichiarazioni locali
                bodyCode,                // Codice dell'espressione
                "stm",                   // Salva il risultato in $tm
                generateRemoveCode(n.declist.size()), // Pulisci dichiarazioni
                "sra",                   // Ripristina return address
                "pop",                   // Rimuovi Access Link (this)
                generateRemoveCode(n.parameters.size()), // Rimuovi parametri
                "sfp",                   // Ripristina frame pointer
                "ltm",                   // Carica il risultato
                "lra",                   // Carica indirizzo ritorno
                "js"                     // Salta indietro
        );

        // Inserisce il codice in FOOLlib
        putCode(methodCode);

        return null;  // i metodi non generano codice quando vengono dichiarati -> quando li chiamerò, salto all'indirizzo puntato dalla dispatchtable
    }

    @Override
    public String visitNode(ClassNode n) {
        if (print) printNode(n, n.id);
        ArrayList<String> dispatchTable = new ArrayList<>(Collections.nCopies(n.methods.size(), ""));
        for (MethodNode m : n.methods) {
            visit(m);
            dispatchTable.set(m.offset, m.label);// metto l'etichetta nella posizione giusta
        }
        String dispatchTableCode = "";
        for (String methodLabel : dispatchTable) {
            dispatchTableCode = nlJoin(dispatchTableCode,
                    "lhp",          // carica l'indirizzo corrente di heap pointer ($hp)
                    "push " + methodLabel, // push l'etichetta del metodo
                    "shp"           // salva etichetta nel heap a indirizzo $hp
            );
        }

        return nlJoin(
                "lhp",       // carico valore corrente di $hp (indirizzo inizio dispatch table)
                dispatchTableCode  // codice per scrivere la dispatch table in heap
        );
    }

    @Override
    public String visitNode(EmptyNode n) {
        if (print) printNode(n, "EmptyNode");
        return "push -1";
    }

    private String generateRemoveCode(int count) {
        StringBuilder removeCode = new StringBuilder();
        for (int i = 0; i < count; i++) {
            removeCode.append("pop\n");
        }
        return removeCode.toString();
    }

    @Override
    public String visitNode(VarNode n) {
        if (print) printNode(n, n.id);
//		visit(n.exp);
//		return null;
        return nlJoin(visit(n.exp));
    }

    @Override
    public String visitNode(PrintNode n) {
        if (print) printNode(n);
//		visit(n.exp);
//		return null;
        return nlJoin(visit(n.exp), "print");
    }

    @Override
    public String visitNode(IfNode n) {
        if (print) printNode(n);
//		visit(n.cond);
//		visit(n.th);
//		visit(n.el);
//		return null;
        String l1 = freshLabel();// salto alla label TODO da rivedere con debug
        String l2 = freshLabel();// salto alla label
        return nlJoin(visit(n.cond), "push 1", "beq " + l1, visit(n.el), "b " + l2, l1 + ":", visit(n.th), l2 + ":");//viene eseguito prima else.
    }

    @Override
    public String visitNode(EqualNode n) {
        if (print) printNode(n);
//		visit(n.left);
//		visit(n.right);
//		return null;
        //return nlJoin();
        String l1 = freshLabel();// salto alla label
        String l2 = freshLabel();// salto alla label
        return nlJoin(visit(n.left), visit(n.right), "beq " + l1, "push 0", "b " + l2, l1 + ":", "push 1", l2 + ":");//false faccio beq al fresh label(creazione), false faccio push di 0(false in int)
    }

    @Override
    public String visitNode(TimesNode n) {
        if (print) printNode(n);
//		visit(n.left);
//		visit(n.right);
//		return null;
        //return nlJoin();
        return nlJoin(visit(n.left), visit(n.right), "mult");
    }

    @Override
    public String visitNode(PlusNode n) {
        if (print) printNode(n);
//		visit(n.left); // cgen left
//		visit(n.right);// cgen right
//		return null;
        return nlJoin(visit(n.left), visit(n.right), "add");
    }

    @Override
    public String visitNode(GreaterEqualNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.right),
                visit(n.left), // a>=b -> b <= a
                "sub",           // calcola right - left
                "push 0",
                "bleq " + l1,     // se >= 0 salta a l1 (right >= left)
                "push 0",        // false
                "b " + l2,
                l1 + ":",
                "push 1",        // true
                l2 + ":"
        );
    }

    @Override
    public String visitNode(LessEqualNode n) {
        if (print) printNode(n);
        // a <= b --> b >= a (scambia left e right)
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "sub",
                "push 0",
                "bleq " + l1,
                "push 0",
                "b " + l2,
                l1 + ":",
                "push 1",
                l2 + ":"
        );
    }

    @Override
    public String visitNode(NotNode n) {
        if (print) printNode(n);
        return nlJoin(
                "push 1",          // NOT(x) = 1 - x
                visit(n.exp),      // calcola valore dell'espressione
                "sub"              // top = 1 - x (se x=1 → 0, se x=0 → 1)
        );
    }


    @Override
    public String visitNode(MinusNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "sub"
        );
    }

    @Override
    public String visitNode(OrNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                visit(n.left),
                "push 1",
                "beq " + l1,       // se left == 1 true subito
                visit(n.right),
                "push 1",
                "beq " + l1,       // se right == 1 true
                "push 0",          // false
                "b " + l2,
                l1 + ":",
                "push 1",          // true
                l2 + ":"
        );
    }

    @Override
    public String visitNode(DivNode n) {
        if (print) printNode(n);
        return nlJoin(
                visit(n.left),
                visit(n.right),
                "div"
        );
    }

    @Override
    public String visitNode(AndNode n) {
        if (print) printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        // short-circuit AND
        return nlJoin(
                visit(n.left),
                "push 0",
                "beq " + l1,        // se left == 0 false subito
                visit(n.right),
                "push 0",
                "beq " + l1,        // se right == 0 false
                "push 1",           // true
                "b " + l2,
                l1 + ":",
                "push 0",           // false
                l2 + ":"
        );
    }


    //TODO da debuggare per capire
    @Override
    public String visitNode(CallNode n) {
        if (print) printNode(n, n.id);
        String getAR = null;
        for (int i = 0; i < n.nl - n.entry.nl; i++)
            getAR = nlJoin(getAR, "lw");// fa la differenza (#TODO guarda g==y)
        String argCode = null;
        for (int i = n.arglist.size() - 1; i >= 0; i--) argCode = nlJoin(argCode, visit(n.arglist.get(i)));
        if (n.entry.offset >= 0) { // funzione.metodo obj.f()
            return nlJoin(
                    "lfp",   // Salva il frame pointer attuale (Access Link del chiamante)
                    argCode,       // Valuta gli argomenti e mettili sullo stack
                    "lfp", getAR,  // Risali nella catena statica fino al frame giusto (contiene il puntatore all’oggetto)
                    "stm",         // Salva in $tm il puntatore all’oggetto (per duplicarlo)
                    "ltm",         // Prima lettura dell’object pointer (Access Link)
                    "ltm",         // Seconda lettura: duplicato (serve per calcolare offset)
                    "lw",          // Carica il dispatch pointer dall’oggetto
                    "push " + n.entry.offset, // Offset del metodo nella dispatch table
                    "add",         // Somma per ottenere indirizzo del metodo
                    "lw",          // Carica indirizzo del metodo
                    "js"           // Salta al metodo
            );
        } else {// funzione normale f(x)
            return nlJoin(
                    "lfp", //metto nello stack il frame point, ho gia sistemato il control link(guarda filetxt), allocazione/visita par inverso ! Posizione attuale
                    argCode,
                    "lfp", getAR,// retrieve address of frame (containing "id" declaration)  // by following the static chain (of Access Links) -> vado a cercarlo
                    "stm",  // set $tm to popped value (with the aim of duplicating top of stack)
                    "ltm",  // load Access Link (pointer to frame of function "id" declaration)
                    "ltm",  // duplicate top of stack //devo duplicare lo stack, la prima volta metto la seconda volta access link(perchè potrebbe essere consumato boh)
                    "push " + n.entry.offset, "add",// compute address of "id" declaration // leggo 2 volte per offset
                    "lw", // load address of "id" function
                    "js"); // jump to popped address (saving address of subsequent instruction in $ra);
        }
    }

    @Override
    public String visitNode(ClassCallNode n) {
        if (print) printNode(n, n.className);

        String argCode = null;
        for (int i = n.args.size() - 1; i >= 0; i--) {
            argCode = nlJoin(argCode, visit(n.args.get(i)));
        }
        // Ricerca classe
        String getAR = null;
        for (int i = 0; i < n.nl - n.classEntry.nl; i++) {
            getAR = nlJoin(getAR, "lw");
        }

        String objCode = nlJoin(
                "lfp",               // carico base static chain
                getAR,                     // risalgo catena statica fino all'AR che contiene ID1
                "push " + n.classEntry.offset,
                "add",                     // calcolo indirizzo di ID1
                "lw"                       // carico valore di ID1 (dispatch pointer)
        );
        // aggiungo ricerca metodo e salto
        return nlJoin(
                "lfp",           // Control Link (ambiente del chiamante)
                argCode,         // Parametri in ordine inverso
                objCode,         // obj = dispatch pointer
                "stm",           // salvo in $tm per duplicare
                "ltm",           // carico prima copia (Access Link), per sapere da dove viene
                "ltm",           // ricarico dispatch pointer, per sapere che metodi ha
                "lw",                                // carico la dispatch table
                "push " + n.methodEntry.offset,      // offset del metodo ID2
                "add",                               // calcolo indirizzo del metodo nella dispatch table
                "lw",                                // carico etichetta
                "js"                                 // salto dal metodo
        );
    }


    @Override
    public String visitNode(IdNode n) {
        if (print) printNode(n, n.id);
        String getAR = null;
        for (int i = 0; i < n.nl - n.entry.nl; i++) getAR = nlJoin(getAR, "lw");// fa la differenza (#TODO guarda g==y)

        return nlJoin("lfp",// retrieve address of frame (containing "id" declaration)
                getAR, // by following the static chain (of Access Links)
                "push " + n.entry.offset,// compute address of "id" declaration // leggo 2 volte per offset
                "add",
                "lw");// load value of "id" variable

        // load fp, push offset, faccio add e li levo dal cima dello stack, poi read the content of the memory cell pointed by the top of the stack
        // and replace the top of the stack with such value, praticamente vado a leggere la variabile
    }

    @Override
    public String visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return "push " + (n.val ? 1 : 0);
    }

    @Override
    public String visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return "push " + n.val;
    }

    @Override
    public String visitNode(ParNode n) {
        if (print) printNode(n, n.id);
        return "push " + n.id;
    }

    @Override
    public String visitNode(NewNode n) throws VoidException {
        if (print) printNode(n, n.className);
        String argCode = null;

        for (int i = n.args.size() - 1; i >= 0; i--) {
            argCode = nlJoin(argCode, visit(n.args.get(i)));
        }

        String copyArgsCode = "";
        for (int i = 0; i < n.args.size(); i++) {
            // pop dal stack e salva in heap[$hp]
            copyArgsCode = nlJoin(copyArgsCode,
                    "stm",        // salva valore top stack in $tm (temporary)
                    "lhp",        // carica $hp (heap pointer)
                    "ltm",        // carica valore da $tm (l’argomento)
                    "shp",        // salva valore argomento in heap[$hp]
                    "lhp",        // carica $hp
                    "push 1",
                    "add",        // $hp = $hp + 1 (incrementa heap pointer)
                    "shp"         // salva aggiornamento heap pointer
            );
        }

        String dispatchPtrCode = nlJoin(
                "lhp",                          // carica $hp (heap pointer corrente)
                "push " + (MEMSIZE + n.classEntry.offset), // carica indirizzo dispatch pointer della classe
                "lw",                           // carica dispatch pointer da memoria
                "shp",                          // scrivi dispatch pointer in heap[$hp]
                "lhp",                          // carica $hp
                "push 1",
                "add",                          // incrementa $hp
                "shp"                           // salva aggiornamento $hp
        );

        String pushObjPtrCode = nlJoin(
                "lhp",           // carica $hp corrente
                "push 1",
                "sub"            // calcola $hp - 1, indirizzo inizio oggetto
        );

        // Tutto il codice unito:
        return nlJoin(
                argCode,           // push argomenti sullo stack
                copyArgsCode,      // copia argomenti dallo stack nell’heap incrementando $hp
                dispatchPtrCode,   // scrivi dispatch pointer e incrementa $hp
                pushObjPtrCode     // carica indirizzo oggetto allocato sullo stack (risultato new)
        );
    }

}

// 	String l1 = freshLabel();


//	String declCode = null;

// generate code for declarations (allocation)


//	String funl = freshFunLabel();


// load Control Link (pointer to frame of function "id" caller)
// generate code for argument expressions in reversed order

// load Access Link (pointer to frame of function "id" declaration)
// duplicate top of stack

// jump to popped address (saving address of subsequent instruction in $ra)
