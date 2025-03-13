package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode = nlJoin(declCode,visit(dec));//joino tutti i codici della dichiarazione
//		visit(n.exp);
// generate code for declarations (allocation)
//		return null;
		return nlJoin("push 0",declCode,visit(n.exp),"halt",getCode());//genera codice per dichiarazioni, push 0 fittizio(guarda file),getcode mette le funzioni in fondo(osserva file.fool.asm)
	}// Formato codice generato: o fittizio, dichiarazioni , contenuto centrale, stop, labels delle funzioni

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
//		visit(n.exp);
//		return null;
		return nlJoin(visit(n.exp),"halt");
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		for (ParNode par : n.parlist) visit(par); //numero di parametri da rimuovere
		String decCode = null;
		for (int i = n.declist.size() - 1; i>=0 ;i--) decCode = nlJoin(decCode, visit(n.declist.get(i)));;//visit(dec);//numero di valori da rimuovere #TODO nella funzione devo disallocare
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
	private String generateRemoveCode(int count) {
		StringBuilder removeCode = new StringBuilder();
		for (int i = 0; i < count; i++) {
			removeCode.append("pop\n");
		}
		return removeCode.toString();
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
//		visit(n.exp);
//		return null;
		return nlJoin(visit(n.exp));
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
//		visit(n.exp);
//		return null;
		return nlJoin(visit(n.exp),"print");
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
		return nlJoin(visit(n.cond),"push 1","beq "+l1,visit(n.el),"b "+ l2,l1+ ":",visit(n.th),l2 +":");//viene eseguito prima else.
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
		return nlJoin(visit(n.left),visit(n.right),"beq " + l1, "push 0", "b "+ l2,l1+ ":","push 1",l2+":");//false faccio beq al fresh label(creazione), false faccio push di 0(false in int)
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
//		visit(n.left);
//		visit(n.right);
//		return null;
		//return nlJoin();
		return nlJoin(visit(n.left),visit(n.right),"mult");
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
//		visit(n.left); // cgen left
//		visit(n.right);// cgen right
//		return null;
		return nlJoin(visit(n.left),visit(n.right),"add");
	}
//TODO da debuggare per capire
	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
//		for (Node arg : n.arglist) visit(arg);
		String getAR = null;
		for (int i = 0; i < n.nl - n.entry.nl; i++) getAR = nlJoin(getAR, "lw");// fa la differenza (#TODO guarda g==y)
		String argCode = null;
		for (int i = n.arglist.size() - 1; i >= 0; i--) argCode = nlJoin(argCode, visit(n.arglist.get(i)));
		return nlJoin(
				"lfp", //metto nello stack il frame point, ho gia sistemato il control link(guarda filetxt), allocazione/visita par inverso !
				argCode,
				"lfp", getAR,// retrieve address of frame (containing "id" declaration)  // by following the static chain (of Access Links)
				"stm",  // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm",  // load Access Link (pointer to frame of function "id" declaration)
				"ltm",  // duplicate top of stack //devo duplicare lo stack, la prima volta metto la seconda volta access link(perchè potrebbe essere consumato boh)
				"push " + n.entry.offset, "add",// compute address of "id" declaration // leggo 2 volte per offset
				"lw", // load address of "id" function
				"js"); // jump to popped address (saving address of subsequent instruction in $ra)
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
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
		if (print) printNode(n,n.val.toString());
		return "push " + (n.val ? 1 : 0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push " + n.val;
	}

	@Override
	public String visitNode(ParNode n){
		if (print) printNode(n,n.toString());
		return "push " + n.id;
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
